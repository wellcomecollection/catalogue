package uk.ac.wellcome.calm_adapter

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl._
import grizzled.slf4j.Logging
import software.amazon.awssdk.services.sqs.model.{Message => SQSMessage}
import uk.ac.wellcome.bigmessaging.FlowOps
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.storage.Version
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.typesafe.Runnable
import weco.catalogue.source_model.CalmSourcePayload

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/** Processes SQS messages consisting of a daily window, and publishes any CALM
  * records to the transformer that have been modified within this window.
  *
  * Consists of the following stages:
  * - Retrieve all CALM records which have modified field the same date as the
  *   window
  * - Store these records in VHS, filtering out ones older than what is
  *   currently in  the store
  * - Publish the VHS key to SNS
  */
class CalmAdapterWorkerService[Destination](
  msgStream: SQSStream[NotificationMessage],
  messageSender: MessageSender[Destination],
  calmRetriever: CalmRetriever,
  calmStore: CalmStore,
  concurrentWindows: Int = 2)(implicit
                              val ec: ExecutionContext,
                              materializer: Materializer)
    extends Runnable
    with FlowOps
    with Logging {

  type Key = Version[String, Int]

  /** Encapsulates context to pass along each akka-stream stage. Newer versions
    * of akka-streams have the asSourceWithContext/ asFlowWithContext idioms for
    * this purpose, which we can migrate to if the library is updated.
    */
  case class Context(msg: SQSMessage)

  val className = this.getClass.getSimpleName

  def run(): Future[Done] =
    msgStream.runStream(
      className,
      source =>
        source
          .via(unwrapMessage)
          .via(processWindow)
          .map { case (Context(msg), _) => msg }
    )

  def unwrapMessage =
    Flow[(SQSMessage, NotificationMessage)]
      .map {
        case (msg, NotificationMessage(body)) =>
          (Context(msg), fromJson[CalmQuery](body).toEither)
      }
      .via(catchErrors)

  /** We process the whole window as a single batch, rather than demultiplexing
    * into stream of individual records. This is because we need to emit exactly
    * one delete action per message received.
    */
  def processWindow =
    Flow[(Context, CalmQuery)]
      .mapAsync(concurrentWindows) {
        case (ctx, query) =>
          info(
            s"Ingesting all Calm records for query: ${query.queryExpression}")
          calmRetriever(query)
            .map(calmStore.putRecord)
            .via(publishKey)
            .via(updatePublished)
            .runWith(Sink.seq)
            .map(checkResultsForErrors(_, query))
            .map((ctx, _))
      }
      .via(catchErrors)

  def publishKey =
    Flow[Result[Option[(Key, S3ObjectLocation, CalmRecord)]]]
      .map {
        case Right(Some((key, location, record))) =>
          val payload = CalmSourcePayload(
            id = key.id,
            location = location,
            version = key.version
          )

          messageSender.sendT(payload) match {
            case Success(_)   => Right(Some((key, record)))
            case Failure(err) => Left(err)
          }
        case Right(None) => Right(None)
        case Left(err)   => Left(err)
      }

  def updatePublished =
    Flow[Result[Option[(Key, CalmRecord)]]]
      .map {
        case Right(Some((key, record))) =>
          calmStore.setRecordPublished(key, record)
        case Right(None) => Right(None)
        case Left(err)   => Left(err)
      }

  def checkResultsForErrors(results: Seq[Result[_]],
                            query: CalmQuery): Result[Unit] = {
    val errs = results.collect { case Left(err) => err }.toList
    if (errs.nonEmpty)
      Left(
        new Exception(
          s"Errors processing query: ${query.queryExpression}: $errs"))
    else
      Right(())
  }
}
