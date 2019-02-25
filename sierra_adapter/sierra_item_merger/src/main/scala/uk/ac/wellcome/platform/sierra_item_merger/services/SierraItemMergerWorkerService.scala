package uk.ac.wellcome.platform.sierra_item_merger.services

import akka.Done
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.SNSWriter
import uk.ac.wellcome.messaging.sqs.NotificationStream
import uk.ac.wellcome.models.transformable.sierra.SierraItemRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.{EmptyMetadata, HybridRecord, VHSIndexEntry}
import uk.ac.wellcome.typesafe.Runnable

import scala.concurrent.{ExecutionContext, Future}

class SierraItemMergerWorkerService(
  notificationStream: NotificationStream[HybridRecord],
  sierraItemMergerUpdaterService: SierraItemMergerUpdaterService,
  objectStore: ObjectStore[SierraItemRecord],
  snsWriter: SNSWriter
)(implicit ec: ExecutionContext)
    extends Runnable {

  def processMessage(hybridRecord: HybridRecord): Future[Unit] =
    for {
      itemRecord <- objectStore.get(hybridRecord.location)
      vhsIndexEntries: Seq[VHSIndexEntry[EmptyMetadata]] <- sierraItemMergerUpdaterService
        .update(itemRecord)
      hybridRecords: Seq[HybridRecord] = vhsIndexEntries.map { _.hybridRecord }
      _ <- Future.sequence(
        hybridRecords.map { hybridRecord =>
          snsWriter.writeMessage(
            message = hybridRecord,
            subject = s"Sent from ${this.getClass.getSimpleName}"
          )
        }
      )
    } yield ()

  def run(): Future[Done] =
    notificationStream.run(processMessage)
}
