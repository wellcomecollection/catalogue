package uk.ac.wellcome.platform.reindex.reindex_worker.fixtures

import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.NotificationStreamFixture
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.platform.reindex.reindex_worker.models.{CompleteReindexParameters, ReindexJobConfig, ReindexParameters, ReindexRequest}
import uk.ac.wellcome.platform.reindex.reindex_worker.services.ReindexWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture
    extends Akka
    with BulkSNSSenderFixture
    with NotificationStreamFixture
    with RecordReaderFixture {
  val defaultJobConfigId = "testing"

  def withWorkerService[R](queue: Queue,
                           configMap: Map[String, (Table, Topic)])(
    testWith: TestWith[ReindexWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withNotificationStream[ReindexRequest, R] { notificationStream =>
        withRecordReader { recordReader =>
          withBulkSNSSender { bulkSNSSender =>
            val workerService = new ReindexWorkerService(
              notificationStream = notificationStream,
              recordReader = recordReader,
              bulkSNSSender = bulkSNSSender,
              reindexJobConfigMap = configMap.map {
                case (key: String, (table: Table, topic: Topic)) =>
                  key -> ReindexJobConfig(
                    dynamoConfig = createDynamoConfigWith(table),
                    snsConfig = createSNSConfigWith(topic)
                  )
              }
            )

            workerService.run()

            testWith(workerService)
          }
        }
      }
    }

  def withWorkerService[R](queue: Queue, table: Table, topic: Topic)(
    testWith: TestWith[ReindexWorkerService, R]): R =
    withWorkerService(
      queue,
      configMap = Map(defaultJobConfigId -> ((table, topic)))) { service =>
      testWith(service)
    }

  private val defaultParameters = CompleteReindexParameters(
    segment = 0,
    totalSegments = 1
  )

  def createReindexRequestWith(
    jobConfigId: String = defaultJobConfigId,
    parameters: ReindexParameters = defaultParameters): ReindexRequest =
    ReindexRequest(
      jobConfigId = jobConfigId,
      parameters = parameters
    )

  def createReindexRequest: ReindexRequest = createReindexRequestWith()
}
