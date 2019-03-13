package uk.ac.wellcome.platform.recorder.fixtures

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.recorder.services.RecorderWorkerService
import uk.ac.wellcome.storage.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.vhs.EmptyMetadata

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends LocalVersionedHybridStore with Messaging {
  def withWorkerService[R](
    table: Table,
    storageBucket: Bucket,
    topic: Topic,
    queue: Queue)(testWith: TestWith[RecorderWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withMetricsSender() { metricsSender =>
        withSNSWriter(topic) { snsWriter =>
          withTypeVHS[TransformedBaseWork, EmptyMetadata, R](
            bucket = storageBucket,
            table = table) { versionedHybridStore =>
            withMessageStream[TransformedBaseWork, R](
              queue = queue,
              metricsSender = metricsSender) { messageStream =>
              val workerService = new RecorderWorkerService(
                versionedHybridStore = versionedHybridStore,
                messageStream = messageStream,
                snsWriter = snsWriter
              )

              workerService.run()

              testWith(workerService)
            }
          }
        }
      }
    }
}
