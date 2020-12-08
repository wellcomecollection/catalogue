package uk.ac.wellcome.platform.sierra_bib_merger.merger

import uk.ac.wellcome.sierra_adapter.model.{
  SierraBibRecord,
  SierraTransformable
}

object BibMerger {

  /** Return the most up-to-date combination of the merged record and the
    * bib record we've just received.
    */
  def mergeBibRecord(
    sierraTransformable: SierraTransformable,
    sierraBibRecord: SierraBibRecord): Option[SierraTransformable] = {
    if (sierraBibRecord.id != sierraTransformable.sierraId) {
      throw new RuntimeException(
        s"Non-matching bib ids ${sierraBibRecord.id} != ${sierraTransformable.sierraId}")
    }

    val isNewerData = sierraTransformable.maybeBibRecord match {
      case Some(bibData) =>
        sierraBibRecord.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      Some(sierraTransformable.copy(maybeBibRecord = Some(sierraBibRecord)))
    } else {
      None
    }
  }
}
