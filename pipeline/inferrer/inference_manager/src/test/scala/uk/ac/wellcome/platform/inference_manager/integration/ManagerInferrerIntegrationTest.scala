package uk.ac.wellcome.platform.inference_manager.integration

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inside, Inspectors, OptionValues}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.ImageGenerators
import uk.ac.wellcome.models.work.internal.{
  AugmentedImage,
  Identified,
  InferredData,
  MergedImage,
  Minted
}
import uk.ac.wellcome.platform.inference_manager.fixtures.InferenceManagerWorkerServiceFixture
import uk.ac.wellcome.platform.inference_manager.services.FeatureVectorInferrerAdapter

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
    with InferenceManagerWorkerServiceFixture[
      MergedImage[Identified, Minted],
      AugmentedImage
    ] {

  it("augments images with feature vectors") {
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), messageSender) =>
        // This is (more than) enough time for the inferrer to have
        // done its prestart work and be ready to use
        eventually(Timeout(scaled(90 seconds))) {
          inferrerIsHealthy shouldBe true
        }

        val image = createIdentifiedMergedImageWith(
          location = createDigitalLocationWith(
            url = "http://image_server/test-image.jpg"
          )
        )
        sendMessage(queue, image)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val augmentedImage = messageSender.getMessages[AugmentedImage].head

          inside(augmentedImage) {
            case AugmentedImage(id, _, _, _, inferredData) =>
              id should be(image.id)
              inside(inferredData.value) {
                case InferredData(features1, features2, lshEncodedFeatures) =>
                  features1 should have length 2048
                  features2 should have length 2048
                  forAll(features1 ++ features2) { _.isNaN shouldBe false }
                  every(lshEncodedFeatures) should fullyMatch regex """(\d+)-(\d+)"""
              }
          }
        }
    }
  }

  val localInferrerPort = 3141

  def inferrerIsHealthy: Boolean = {
    val source =
      Source.fromURL(s"http://localhost:$localInferrerPort/healthcheck")
    try source.mkString.nonEmpty
    catch { case _: Exception => false } finally source.close()
  }

  def withWorkerServiceFixtures[R](
    testWith: TestWith[(QueuePair, MemoryMessageSender), R]): R =
    // We would like a timeout longer than 1s here because the inferrer
    // may need to warm up.
    withLocalSqsQueuePair(visibilityTimeout = 5) { queuePair =>
      val messageSender = new MemoryMessageSender()

      withWorkerService(
        queuePair.queue,
        messageSender,
        FeatureVectorInferrerAdapter,
        localInferrerPort) { _ =>
        testWith((queuePair, messageSender))
      }
    }
}
