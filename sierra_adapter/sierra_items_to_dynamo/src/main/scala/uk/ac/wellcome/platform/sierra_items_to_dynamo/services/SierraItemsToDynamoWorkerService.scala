package uk.ac.wellcome.platform.sierra_items_to_dynamo.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.sierra_adapter.model.SierraItemRecord
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.Future
import scala.util.Success

class SierraItemsToDynamoWorkerService[Destination](
  sqsStream: SQSStream[NotificationMessage],
  dynamoInserter: DynamoInserter,
  messageSender: MessageSender[Destination]
) extends Runnable {

  private def process(message: NotificationMessage): Future[Unit] =
    Future.fromTry {
      for {
        itemRecord <- fromJson[SierraItemRecord](message.body)
        key <- dynamoInserter.insertIntoDynamo(itemRecord).toTry
        _ <- key match {
          case Some(k) => messageSender.sendT(k)
          case None => Success(())
        }
      } yield ()
    }

  def run(): Future[Done] =
    sqsStream.foreach(this.getClass.getSimpleName, process)
}
