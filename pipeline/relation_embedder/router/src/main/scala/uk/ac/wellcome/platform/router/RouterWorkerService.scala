package uk.ac.wellcome.platform.router

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.models.work.internal.WorkState.Merged
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.pipeline_storage.Retriever
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class RouterWorkerService[MsgDestination](
  sqsStream: SQSStream[NotificationMessage],
  worksMsgSender: MessageSender[MsgDestination],
  pathsMsgSender: MessageSender[MsgDestination],
  workRetriever: Retriever[Work[Merged]]
)(implicit ec: ExecutionContext)
    extends Runnable {

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, processMessage)

  private def processMessage(message: NotificationMessage): Future[Unit] = {
    workRetriever.apply(message.body).flatMap { work =>
      work.data.collectionPath
        .fold(ifEmpty = Future.fromTry(worksMsgSender.sendT(work.id))) { path =>
          Future.fromTry(pathsMsgSender.sendT(path))
        }
    }
  }
}
