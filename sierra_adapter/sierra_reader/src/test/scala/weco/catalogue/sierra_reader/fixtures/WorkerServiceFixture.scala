package weco.catalogue.sierra_reader.fixtures

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.sierra_reader.config.models.ReaderConfig
import uk.ac.wellcome.platform.sierra_reader.services.SierraReaderWorkerService
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket
import weco.catalogue.source_model.sierra.identifiers.SierraRecordTypes
import weco.http.client.{HttpGet, MemoryHttpClient}

import scala.concurrent.ExecutionContext.Implicits.global

trait WorkerServiceFixture extends Akka with SQS with S3Fixtures {

  val sierraUri = "http://sierra:1234"

  def createClient(responses: Seq[(HttpRequest, HttpResponse)]): HttpGet =
    new MemoryHttpClient(responses) with HttpGet {
      override val baseUri: Uri = Uri(sierraUri)
    }

  def withWorkerService[R](
    responses: Seq[(HttpRequest, HttpResponse)],
    bucket: Bucket,
    queue: Queue,
    readerConfig: ReaderConfig = bibsReaderConfig
  )(testWith: TestWith[SierraReaderWorkerService, R]): R =
    withActorSystem { implicit actorSystem =>
      withSQSStream[NotificationMessage, R](queue) { sqsStream =>
        val workerService = new SierraReaderWorkerService(
          client = createClient(responses),
          sqsStream = sqsStream,
          s3Config = createS3ConfigWith(bucket),
          readerConfig = readerConfig
        )

        workerService.run()

        testWith(workerService)
      }
    }

  val bibsReaderConfig: ReaderConfig = ReaderConfig(
    recordType = SierraRecordTypes.bibs,
    fields = "updatedDate,deletedDate,deleted,suppressed,author,title"
  )

  val itemsReaderConfig: ReaderConfig = ReaderConfig(
    recordType = SierraRecordTypes.items,
    fields = "updatedDate,deleted,deletedDate,bibIds,fixedFields,varFields"
  )

  val holdingsReaderConfig: ReaderConfig = ReaderConfig(
    recordType = SierraRecordTypes.holdings,
    fields = "updatedDate"
  )
}