package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.platform.sierra_items_to_dynamo.fixtures.WorkerServiceFixture
import uk.ac.wellcome.platform.sierra_items_to_dynamo.merger.SierraItemRecordMerger
import uk.ac.wellcome.sierra_adapter.model.SierraGenerators
import uk.ac.wellcome.sierra_adapter.utils.SierraAdapterHelpers
import uk.ac.wellcome.storage.Version

class SierraItemsToDynamoWorkerServiceTest
    extends AnyFunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with ScalaFutures
    with SierraGenerators
    with WorkerServiceFixture
    with SierraAdapterHelpers {

  it("reads a sierra record from SQS and inserts it into DynamoDB") {
    val bibIds = createSierraBibNumbers(count = 5)

    val bibIds1 = List(bibIds(0), bibIds(1), bibIds(2))

    val record1 = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds1
    )

    val bibIds2 = List(bibIds(2), bibIds(3), bibIds(4))

    val record2 = createSierraItemRecordWith(
      id = record1.id,
      modifiedDate = newerDate,
      bibIds = bibIds2
    )

    val expectedRecord = SierraItemRecordMerger
      .mergeItems(
        existingRecord = record1,
        updatedRecord = record2
      )
      .get

    val sourceVHS = createSourceVHSWith(
      initialEntries = Map(
        Version(record1.id.withoutCheckDigit, 1) -> record1
      )
    )

    withLocalSqsQueue() { queue =>
      withWorkerService(queue, sourceVHS) { _ =>
        sendNotificationToSQS(queue = queue, message = record2)

        eventually {
          assertStored(record1.id.withoutCheckDigit, expectedRecord, sourceVHS)
        }
      }
    }
  }

  it("only applies an update once, even if it's sent multiple times") {
    val bibIds = createSierraBibNumbers(count = 5)

    val bibIds1 = List(bibIds(0), bibIds(1), bibIds(2))

    val record1 = createSierraItemRecordWith(
      modifiedDate = olderDate,
      bibIds = bibIds1
    )

    val bibIds2 = List(bibIds(2), bibIds(3), bibIds(4))

    val record2 = createSierraItemRecordWith(
      id = record1.id,
      modifiedDate = newerDate,
      bibIds = bibIds2
    )

    val expectedRecord = SierraItemRecordMerger
      .mergeItems(
        existingRecord = record1,
        updatedRecord = record2
      )
      .get

    val sourceVHS = createSourceVHSWith(
      initialEntries = Map(
        Version(record1.id.withoutCheckDigit, 1) -> record1
      )
    )

    withLocalSqsQueuePair() {
      case QueuePair(queue, dlq) =>
        withWorkerService(queue, sourceVHS) {
          case (_, messageSender) =>
            (1 to 5).foreach { _ =>
              sendNotificationToSQS(queue = queue, message = record2)
            }

            eventually {
              assertQueueEmpty(queue)
              assertQueueEmpty(dlq)

              messageSender.getMessages[Version[String, Int]]() shouldBe List(
                Version(record1.id.withoutCheckDigit, 2)
              )

              assertStored(record1.id.withoutCheckDigit, expectedRecord, sourceVHS)
            }
        }
    }
  }

  it("records a failure if it receives an invalid message") {
    val metrics = new MemoryMetrics()
    withLocalSqsQueuePair() {
      case QueuePair(queue, dlq) =>
        withWorkerService(queue, metrics = metrics) { _ =>
          val body =
            """
                    |{
                    | "something": "something"
                    |}
                  """.stripMargin

          sendNotificationToSQS(queue = queue, body = body)

          eventually {
            assertQueueEmpty(queue)
            assertQueueHasSize(dlq, size = 1)
            metrics.incrementedCounts should not contain "SierraItemsToDynamoWorkerService_ProcessMessage_failure"
          }
        }
    }
  }
}
