package uk.ac.wellcome.platform.transformer.miro.services

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.transformer.miro.exceptions.MiroTransformerException
import uk.ac.wellcome.platform.transformer.miro.fixtures.MiroVHSRecordReceiverFixture
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SNS.Topic

import scala.util.Try

class MiroVHSRecordReceiverTest
    extends AnyFunSpec
    with Matchers
    with MiroVHSRecordReceiverFixture
    with IntegrationPatience
    with ScalaFutures
    with MiroRecordGenerators
    with WorksGenerators {

  case class TestException(message: String) extends Exception(message)

  def transformToWork(miroRecord: MiroRecord,
                      metadata: MiroMetadata,
                      version: Int) =
    Try(createUnidentifiedWorkWith(version = version))

  def failingTransformToWork(miroRecord: MiroRecord,
                             metadata: MiroMetadata,
                             version: Int) =
    Try(throw TestException("BOOOM!"))

  it("receives a message and sends it to SNS client") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message = createHybridRecordNotification

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future) { _ =>
            val works = getMessages[TransformedBaseWork](topic)
            works.size should be >= 1

            works.map { work =>
              work shouldBe a[UnidentifiedWork]
            }
          }
        }
      }
    }
  }

  it("receives a message and adds the version to the transformed work") {
    val version = 5

    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message = createHybridRecordNotificationWith(
          version = version
        )

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future) { _ =>
            val works = getMessages[TransformedBaseWork](topic)
            works.size should be >= 1

            works.map { actualWork =>
              actualWork shouldBe a[UnidentifiedWork]
              val unidentifiedWork = actualWork.asInstanceOf[UnidentifiedWork]
              unidentifiedWork.version shouldBe version
            }
          }
        }
      }
    }
  }

  // It's not possible to store a record without metadata with the HybridStore
  // used in these tests
  ignore("returns a failed future if there's no MiroMetadata") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val incompleteMessage = createHybridRecordNotification

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(incompleteMessage, transformToWork)

          whenReady(future.failed) {
            _ shouldBe a[MiroTransformerException]
          }
        }
      }
    }
  }

  it("returns a failed future if there's no HybridRecord") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val incompleteMessage = createNotificationMessageWith(
          message = MiroMetadata(isClearedForCatalogueAPI = false)
        )

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(incompleteMessage, transformToWork)

          whenReady(future.failed) {
            _ shouldBe a[MiroTransformerException]
          }
        }
      }
    }
  }

  it("fails if it's unable to perform a transformation") {
    withLocalSnsTopic { topic =>
      withLocalS3Bucket { bucket =>
        val message = createHybridRecordNotification

        withMiroVHSRecordReceiver(topic, bucket) { recordReceiver =>
          val future =
            recordReceiver.receiveMessage(message, failingTransformToWork)

          whenReady(future.failed) {
            _ shouldBe a[TestException]
          }
        }
      }
    }
  }

  it("fails if it's unable to publish the work") {
    withLocalS3Bucket { bucket =>
      val message = createHybridRecordNotification

      withMiroVHSRecordReceiver(Topic("no-such-topic"), bucket) {
        recordReceiver =>
          val future = recordReceiver.receiveMessage(message, transformToWork)

          whenReady(future.failed) {
            _.getMessage should include("Unknown topic: no-such-topic")
          }
      }
    }
  }
}
