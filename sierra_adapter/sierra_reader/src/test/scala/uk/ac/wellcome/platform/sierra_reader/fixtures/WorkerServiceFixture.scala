package uk.ac.wellcome.platform.sierra_reader.fixtures

import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.sierra_reader.config.models.{
  ReaderConfig,
  SierraAPIConfig
}
import uk.ac.wellcome.platform.sierra_reader.models.SierraResourceTypes
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends Akka with SQS with S3Fixtures {
  def withWorkerService[R](bucket: Bucket,
                           queue: Queue,
                           readerConfig: ReaderConfig = bibsReaderConfig,
                           sierraAPIConfig: SierraAPIConfig = sierraAPIConfig)(
    testWith: TestWith[SierraReaderWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue) { sqsStream =>
        val workerService = new SierraReaderWorkerService(
          sqsStream = sqsStream,
          s3client = s3Client,
          s3Config = createS3ConfigWith(bucket),
          readerConfig = readerConfig,
          sierraAPIConfig = sierraAPIConfig
        )

        workerService.run()

        testWith(workerService)
      }
    }

  val bibsReaderConfig: ReaderConfig = ReaderConfig(
    resourceType = SierraResourceTypes.bibs,
    fields = "updatedDate,deletedDate,deleted,suppressed,author,title"
  )

  val itemsReaderConfig: ReaderConfig = ReaderConfig(
    resourceType = SierraResourceTypes.items,
    fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields"
  )

  val sierraAPIConfig: SierraAPIConfig = SierraAPIConfig(
    apiURL = "http://localhost:8080",
    oauthKey = "key",
    oauthSec = "secret"
  )
}
