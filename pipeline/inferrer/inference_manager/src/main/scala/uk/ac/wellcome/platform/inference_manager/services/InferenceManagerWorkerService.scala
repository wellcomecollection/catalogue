package uk.ac.wellcome.platform.inference_manager.services

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.Done
import akka.http.scaladsl.model.HttpResponse
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, FlowWithContext, Source}
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.sqs.model.Message

import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.models.work.internal.{Image, ImageState, InferredData}
import uk.ac.wellcome.platform.inference_manager.adapters.{
  InferrerAdapter,
  InferrerResponse
}
import uk.ac.wellcome.platform.inference_manager.models.DownloadedImage
import uk.ac.wellcome.typesafe.Runnable
import uk.ac.wellcome.pipeline_storage.{
  Indexable,
  Indexer,
  PipelineStorageConfig,
  PipelineStorageStream,
  Retriever
}
import uk.ac.wellcome.json.JsonUtil._
import ImageState.{Augmented, Initial}
import Indexable.imageIndexable

case class AdapterResponseBundle[ImageType](
  image: ImageType,
  adapter: InferrerAdapter,
  response: Try[InferrerResponse]
)

class InferenceManagerWorkerService[Destination](
  msgStream: SQSStream[NotificationMessage],
  msgSender: MessageSender[Destination],
  imageRetriever: Retriever[Image[Initial]],
  imageIndexer: Indexer[Image[Augmented]],
  pipelineStorageConfig: PipelineStorageConfig,
  imageDownloader: ImageDownloader[Message],
  inferrerAdapters: Set[InferrerAdapter],
  requestPool: RequestPoolFlow[(DownloadedImage, InferrerAdapter), Message]
)(implicit actorSystem: ActorSystem, ec: ExecutionContext)
    extends Runnable
    with Logging {

  val className: String = this.getClass.getSimpleName
  val parallelism = 10
  val maxInferrerWait = 30 seconds
  val maxOpenRequests = actorSystem.settings.config
    .getInt("akka.http.host-connection-pool.max-open-requests")

  val indexAndSend = PipelineStorageStream.batchIndexAndSendFlow(
    pipelineStorageConfig,
    (image: Image[Augmented]) => msgSender.send(imageIndexable.id(image)),
    imageIndexer
  )

  def run(): Future[Done] =
    for {
      _ <- imageIndexer.init()
      _ <- msgStream.runStream(
        className,
        source =>
          source
            .asSourceWithContext { case (message, _) => message }
            .mapAsync(5) {
              case (_, message) => imageRetriever(message.body)
            }
            .via(imageDownloader.download)
            .via(createRequests)
            .via(requestPool.asContextFlow)
            .via(unmarshalResponse)
            .via(collectAndAugment)
            .asSource
            .map { case (image, message) => (message, List(image)) }
            .via(indexAndSend)
      )
    } yield Done

  private def createRequests[Ctx] =
    FlowWithContext[DownloadedImage, Ctx].mapConcat { image =>
      inferrerAdapters.map { inferrerAdapter =>
        (
          inferrerAdapter.createRequest(image),
          (image, inferrerAdapter)
        )
      }
    }

  private def unmarshalResponse[Ctx] =
    FlowWithContext[
      (Try[HttpResponse], (DownloadedImage, InferrerAdapter)),
      Ctx]
      .mapAsync(parallelism) {
        case (Success(response), (image, adapter)) =>
          adapter
            .parseResponse(response)
            .map(Success(_))
            .recover {
              case e: Exception =>
                response.discardEntityBytes()
                Failure(e)
            }
            .map { response =>
              AdapterResponseBundle(
                image,
                adapter,
                response
              )
            }
        case (Failure(exception), (image, _)) =>
          imageDownloader.delete.runWith(Source.single(image))
          Future.failed(exception)
      }

  private def collectAndAugment[Ctx <: Message] =
    FlowWithContext[AdapterResponseBundle[DownloadedImage], Ctx]
      .via {
        Flow[(AdapterResponseBundle[DownloadedImage], Ctx)]
          .groupBy(maxOpenRequests * inferrerAdapters.size, _ match {
            case (_, msg) => msg.messageId()
          }, allowClosedSubstreamRecreation = true)
          .groupedWithin(inferrerAdapters.size, maxInferrerWait)
          .take(1)
          .map { elements =>
            elements.foreach {
              case (AdapterResponseBundle(image, _, _), _) =>
                imageDownloader.delete.runWith(Source.single(image))
            }
            elements
          }
          .map { elements =>
            elements.filter {
              case (AdapterResponseBundle(_, _, Failure(_)), _) => false
              case _                                            => true
            }
          }
          .map { elements =>
            if (elements.size != inferrerAdapters.size) {
              val failedInferrers =
                inferrerAdapters.map(_.getClass.getSimpleName) --
                  elements.map(_._1.adapter.getClass.getSimpleName).toSet
              throw new Exception(
                s"Did not receive responses from $failedInferrers within $maxInferrerWait")
            }
            elements
          }
          .map { elements =>
            val inferredData = elements.foldLeft(InferredData.empty) {
              case (
                  data,
                  (AdapterResponseBundle(_, adapter, Success(response)), _)) =>
                adapter.augment(data, response.asInstanceOf[adapter.Response])
            }
            elements.head match {
              case (
                  AdapterResponseBundle(DownloadedImage(image, _), _, _),
                  ctx) =>
                (
                  image.transition[ImageState.Augmented](Some(inferredData)),
                  ctx)
            }
          }
          .mergeSubstreamsWithParallelism(
            maxOpenRequests * inferrerAdapters.size
          )
      }
}
