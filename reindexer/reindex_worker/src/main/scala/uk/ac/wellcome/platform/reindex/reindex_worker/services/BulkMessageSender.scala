package uk.ac.wellcome.platform.reindex.reindex_worker.services

import uk.ac.wellcome.messaging.IndividualMessageSender

import scala.concurrent.{ExecutionContext, Future}

class BulkMessageSender[Destination](
  messageSender: IndividualMessageSender[Destination]
)(
  implicit ec: ExecutionContext) {
  def send(messages: Seq[String], destination: Destination): Future[Seq[Unit]] = {
    Future.sequence {
      messages
        .map {
          body =>
            Future.fromTry { messageSender.send(body)(
              subject = "Sent from reindex_worker",
              destination = destination
            ) }
        }
    }
  }
}
