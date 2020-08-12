package uk.ac.wellcome.platform.inference_manager.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, Inside, Inspectors, OptionValues}
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.work.internal.{
  AugmentedImage,
  Identified,
  InferredData,
  MergedImage,
  Minted
}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.ImageGenerators
import uk.ac.wellcome.platform.inference_manager.fixtures.{
  FeatureVectorInferrerMock,
  InferenceManagerWorkerServiceFixture,
  InferrerWiremock
}

class InferenceManagerWorkerServiceTest
    extends AnyFunSpec
    with Matchers
    with ImageGenerators
    with Inside
    with OptionValues
    with Inspectors
    with BeforeAndAfterAll
    with Eventually
    with IntegrationPatience
    with InferenceManagerWorkerServiceFixture[
      MergedImage[Identified, Minted],
      AugmentedImage
    ] {

  val inferrerMock = new InferrerWiremock(FeatureVectorInferrerMock)

  override def beforeAll(): Unit = {
    inferrerMock.start()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    inferrerMock.stop()
    super.afterAll()
  }

  it(
    "reads image messages, augments them with the inferrer, and sends them to SNS") {
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), messageSender) =>
        val image = createIdentifiedMergedImageWith()
        sendMessage(queue, image)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val augmentedWork = messageSender.getMessages[AugmentedImage].head

          inside(augmentedWork) {
            case AugmentedImage(id, _, _, _, inferredData) =>
              id should be(image.id)
              inside(inferredData.value) {
                case InferredData(features1, features2, lshEncodedFeatures) =>
                  features1 should have length 2048
                  features2 should have length 2048
                  every(lshEncodedFeatures) should fullyMatch regex """(\d+)-(\d+)"""
              }
          }
        }
    }
  }

  it("places images that fail inference deterministically on the DLQ") {
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), _) =>
        val image404 = createIdentifiedMergedImageWith(
          location = createDigitalLocationWith(url = "lost_image")
        )
        val image400 = createIdentifiedMergedImageWith(
          location = createDigitalLocationWith(url = "malformed_image_url")
        )
        sendMessage(queue, image404)
        sendMessage(queue, image400)
        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 2)
        }
    }
  }

  it("allows images that fail inference nondeterministically to pass through") {
    withWorkerServiceFixtures {
      case (QueuePair(queue, dlq), messageSender) =>
        val image500 = createIdentifiedMergedImageWith(
          location = createDigitalLocationWith(url = "extremely_cursed_image")
        )
        sendMessage(queue, image500)
        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val output = messageSender.getMessages[AugmentedImage].head

          inside(output) {
            case AugmentedImage(id, _, _, _, inferredData) =>
              id should be(image500.id)
              inferredData should not be defined
          }
        }
    }
  }

  def withWorkerServiceFixtures[R](
    testWith: TestWith[(QueuePair, MemoryMessageSender), R]): R =
    withLocalSqsQueuePair() { queuePair =>
      val messageSender = new MemoryMessageSender()

      withWorkerService(
        queuePair.queue,
        messageSender,
        FeatureVectorInferrerAdapter,
        inferrerMock.port) { _ =>
        testWith((queuePair, messageSender))
      }
    }
}
