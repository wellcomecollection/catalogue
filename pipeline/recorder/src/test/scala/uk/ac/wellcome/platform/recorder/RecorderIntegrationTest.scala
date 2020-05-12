package uk.ac.wellcome.platform.recorder

import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.fixtures.WorkerServiceFixture
import uk.ac.wellcome.bigmessaging.fixtures.BigMessagingFixture
import uk.ac.wellcome.bigmessaging.typesafe.VHSBuilder
import uk.ac.wellcome.storage.fixtures.DynamoFixtures
import uk.ac.wellcome.storage.dynamo.DynamoConfig
import uk.ac.wellcome.storage.ObjectLocationPrefix
import uk.ac.wellcome.storage.Version

class RecorderIntegrationTest
    extends AnyFunSpec
    with Matchers
    with IntegrationPatience
    with DynamoFixtures
    with BigMessagingFixture
    with WorkerServiceFixture
    with WorksGenerators {

  override def createTable(
    table: DynamoFixtures.Table): DynamoFixtures.Table = {
    createTableWithHashKey(
      table,
      keyName = "id",
      keyType = ScalarAttributeType.S
    )
  }

  it("saves received works to VHS, and puts the VHS key on the queue") {
    withLocalSqsQueue() { queue =>
      withLocalS3Bucket { bucket =>
        withLocalDynamoDbTable { table =>
          withLocalSnsTopic { topic =>
            withSnsMessageSender(topic) { msgSender =>
              val vhs = VHSBuilder.build[TransformedBaseWork](
                ObjectLocationPrefix(
                  namespace = bucket.name,
                  path = "recorder"),
                DynamoConfig(table.name, table.index),
                dynamoClient,
                s3Client,
              )
              withWorkerService(queue, vhs, msgSender) { service =>
                val work = createUnidentifiedWork
                sendMessage[TransformedBaseWork](queue = queue, obj = work)
                eventually {
                  val key = assertWorkStored(vhs, work)
                  val messages = listMessagesReceivedFromSNS(topic)
                    .map(_.message)
                    .map(fromJson[Version[String, Int]](_).get)
                  messages.toList shouldBe List(key)
                }
              }
            }
          }
        }
      }
    }
  }
}
