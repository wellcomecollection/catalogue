package weco.pipeline.sierra_merger.services

import cats.implicits._
import weco.catalogue.source_model.sierra.identifiers.SierraBibNumber
import weco.catalogue.source_model.sierra.{
  AbstractSierraRecord,
  SierraTransformable
}
import weco.catalogue.source_model.store.SourceVHS
import weco.pipeline.sierra_merger.models.{RecordOps, TransformableOps}
import weco.storage.s3.S3ObjectLocation
import weco.storage.{Identified, StorageError, UpdateNotApplied, Version}

class Updater[Record <: AbstractSierraRecord[_]](
  sourceVHS: SourceVHS[SierraTransformable]
)(
  implicit
  transformableOps: TransformableOps[Record],
  recordOps: RecordOps[Record]
) {
  import weco.pipeline.sierra_merger.models.RecordOps._
  import weco.pipeline.sierra_merger.models.TransformableOps._

  def update(record: Record)
    : Either[StorageError,
             List[Identified[Version[String, Int], S3ObjectLocation]]] = {
    val linkUpdates =
      record.linkedBibIds.map { linkBib(_, record) }

    val unlinkUpdates =
      record.unlinkedBibIds.map { unlinkBib(_, record) }

    (linkUpdates ++ unlinkUpdates).filter {
      case Left(_: UpdateNotApplied) => false
      case _                         => true
    }.sequence
  }

  private def linkBib(bibId: SierraBibNumber, record: Record): Either[
    StorageError,
    Identified[Version[String, Int], S3ObjectLocation]] = {
    val newTransformable = transformableOps.create(bibId, record)

    sourceVHS
      .upsert(bibId.withoutCheckDigit)(newTransformable) {
        _.add(record) match {
          case Some(updatedRecord) => Right(updatedRecord)
          case None =>
            Left(
              UpdateNotApplied(
                new Throwable(s"Bib $bibId is already up to date")))
        }
      }
      .map { case Identified(id, (location, _)) => Identified(id, location) }
  }

  private def unlinkBib(unlinkedBibId: SierraBibNumber, record: Record)
    : Either[StorageError, Identified[Version[String, Int], S3ObjectLocation]] =
    sourceVHS
      .update(unlinkedBibId.withoutCheckDigit) {
        _.remove(record) match {
          case Some(updatedRecord) => Right(updatedRecord)
          case None =>
            Left(
              UpdateNotApplied(
                new Throwable(s"Bib $unlinkedBibId is already up to date")))
        }
      }
      .map { case Identified(id, (location, _)) => Identified(id, location) }
}
