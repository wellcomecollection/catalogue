package uk.ac.wellcome.platform.inference_manager.integration

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.Http
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, Inspectors, OptionValues}
import software.amazon.awssdk.services.sqs.model.Message
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.ImageGenerators
import uk.ac.wellcome.models.work.internal.{AugmentedImage, InferredData}
import uk.ac.wellcome.platform.inference_manager.adapters.{
  FeatureVectorInferrerAdapter,
  InferrerAdapter,
  PaletteInferrerAdapter
}
import uk.ac.wellcome.platform.inference_manager.fixtures.InferenceManagerWorkerServiceFixture
import uk.ac.wellcome.platform.inference_manager.models.DownloadedImage
import uk.ac.wellcome.platform.inference_manager.services.{
  DefaultFileWriter,
  MergedIdentifiedImage
}

import scala.concurrent.duration._
import scala.io.Source

class ManagerInferrerIntegrationTest
    extends AnyFunSpec
    with Matchers
    with ImageGenerators
    with OptionValues
    with Inside
    with Inspectors
    with Eventually
    with IntegrationPatience
    with InferenceManagerWorkerServiceFixture {

  it("augments images with feature and palette vectors") {
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), messageSender, rootDir) =>
        // This is (more than) enough time for the inferrer to have
        // done its prestart work and be ready to use
        eventually(Timeout(scaled(90 seconds))) {
          inferrersAreHealthy shouldBe true
        }

        val image = createIdentifiedImageWith(
          locations = List(
            createDigitalLocationWith(
              locationType = createImageLocationType,
              url = s"http://localhost:$localImageServerPort/test-image.jpg"
            ))
        )
        sendMessage(queue, image)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          rootDir.listFiles().length should be(0)

          val augmentedImage = messageSender.getMessages[AugmentedImage].head

          inside(augmentedImage) {
            case AugmentedImage(id, _, _, _, _, Some(inferredData)) =>
              id should be(image.id)
              inside(inferredData) {
                case InferredData(
                    features1,
                    features2,
                    lshEncodedFeatures,
                    palette) =>
                  features1 should have length 2048
                  features2 should have length 2048
                  forAll(features1 ++ features2) { _.isNaN shouldBe false }
                  every(lshEncodedFeatures) should fullyMatch regex """(\d+)-(\d+)"""
                  every(palette) should fullyMatch regex """\d+/\d+"""
              }
          }
        }
    }
  }

  val localInferrerPorts: Map[String, Int] = Map(
    "feature" -> 3141,
    "palette" -> 3142
  )
  val localImageServerPort = 2718

  def inferrersAreHealthy: Boolean =
    localInferrerPorts.values.forall { port =>
      val source =
        Source.fromURL(s"http://localhost:$port/healthcheck")
      try source.mkString.nonEmpty
      catch {
        case _: Exception => false
      } finally source.close()
    }

  def withWorkerServiceFixtures[R](
    testWith: TestWith[(QueuePair, MemoryMessageSender, File), R]): R =
    // We would like a timeout longer than 1s here because the inferrer
    // may need to warm up.
    withLocalSqsQueuePair(visibilityTimeout = 5) { queuePair =>
      val messageSender = new MemoryMessageSender()
      val root = Paths.get("integration-tmp").toFile
      root.mkdir()
      withActorSystem { implicit actorSystem =>
        withWorkerService(
          queuePair.queue,
          messageSender,
          adapters = Set(
            new FeatureVectorInferrerAdapter(
              "localhost",
              localInferrerPorts("feature")),
            new PaletteInferrerAdapter(
              "localhost",
              localInferrerPorts("palette")
            ),
          ),
          fileWriter = new DefaultFileWriter(root.getPath),
          inferrerRequestPool =
            Http().superPool[((DownloadedImage, InferrerAdapter), Message)](),
          imageRequestPool =
            Http().superPool[(MergedIdentifiedImage, Message)](),
          fileRoot = root.getPath
        ) { _ =>
          try {
            testWith((queuePair, messageSender, root))
          } finally {
            root.delete()
          }
        }
      }
    }
}
