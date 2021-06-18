package weco.tei.adapter;

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.storage.Version
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.storage.store.memory.MemoryVersionedStore
import weco.catalogue.source_model.TeiSourcePayload
import weco.catalogue.source_model.tei.{TeiIdChangeMessage, TeiMetadata}

import java.time.Instant
import java.time.temporal.ChronoUnit

class TeiAdapterWorkerServiceTest extends AnyFunSpec with SQS with Eventually with Akka with IntegrationPatience{
    it("processes a message from the tei id extractor"){
      withLocalSqsQueuePair() { case QueuePair(queue, dlq) =>
        withActorSystem { implicit ac =>
          implicit val ec = ac.dispatcher
          withSQSStream(queue) { stream: SQSStream[NotificationMessage] =>
            val store = MemoryVersionedStore[String,TeiMetadata](Map.empty)
            val messageSender = new MemoryMessageSender()
            val bucket = "bucket"
            val key = "key.xml"
            val message = TeiIdChangeMessage("manuscript_1234", S3ObjectLocation(bucket, key), Instant.parse("2021-06-17T11:46:00Z"))

            sendNotificationToSQS(queue, message)
            val service = new TeiAdapterWorkerService(stream, messageSender, store)
            service.run()

            eventually {
              val expectedVersion = Version(message.id, 0)
              messageSender.getMessages[Version[String, Int]]() should contain only expectedVersion
              val expectedMetadata = TeiMetadata(deleted = false, s3Location = message.s3Location, timeModified = message.timeModified)
              messageSender.getMessages[TeiSourcePayload] should contain only (TeiSourcePayload(message.id,expectedMetadata, 0))
              val stored = store.getLatest(message.id).right.get
              stored.identifiedT shouldBe expectedMetadata
              stored.id shouldBe expectedVersion
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
            }
          }
        }
      }
    }

    it("updates the version if it gets a new message for an id already stored"){
      withLocalSqsQueuePair() { case QueuePair(queue, dlq) =>
        withActorSystem { implicit ac =>
          implicit val ec = ac.dispatcher
          withSQSStream(queue) { stream: SQSStream[NotificationMessage] =>

            val messageSender = new MemoryMessageSender()
            val bucket = "bucket"
            val key = "key.xml"
            val message = TeiIdChangeMessage("manuscript_1234", S3ObjectLocation(bucket, key), Instant.parse("2021-06-17T11:46:00Z"))
            val oldVersion = Version(message.id, 0)
            val store = MemoryVersionedStore[String,TeiMetadata](Map(oldVersion-> TeiMetadata(false, message.s3Location, message.timeModified.minus(2, ChronoUnit.HOURS))))

            sendNotificationToSQS(queue, message)
            val service = new TeiAdapterWorkerService(stream, messageSender, store)
            service.run()

            eventually {
              val expectedVersion = Version(message.id, 1)
              messageSender.getMessages[Version[String, Int]]() should contain only expectedVersion

              val expectedMetadata = TeiMetadata(deleted = false, s3Location = message.s3Location, timeModified = message.timeModified)
              messageSender.getMessages[TeiSourcePayload] should contain only (TeiSourcePayload(message.id,expectedMetadata, expectedVersion.version))

              val stored = store.getLatest(message.id).right.get
              stored.identifiedT shouldBe expectedMetadata
              stored.id shouldBe expectedVersion

              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)
            }
          }
        }
      }
    }
}
