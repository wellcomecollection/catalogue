package uk.ac.wellcome.platform.sierra_item_merger.links

import uk.ac.wellcome.sierra_adapter.model.{
  SierraItemRecord,
  SierraTransformable
}

object ItemMerger {

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns the merged record.
    */
  def mergeItemRecord(
    sierraTransformable: SierraTransformable,
    itemRecord: SierraItemRecord): Option[SierraTransformable] = {
    if (!itemRecord.bibIds.contains(sierraTransformable.sierraId)) {
      throw new RuntimeException(
        s"Non-matching bib id ${sierraTransformable.sierraId} in item bib ${itemRecord.bibIds}")
    }

    // We can decide whether to insert the new data in two steps:
    //
    //  - Do we already have any data for this item?  If not, we definitely
    //    need to merge this record.
    //  - If we have existing data, is it newer or older than the update we've
    //    just received?  If the existing data is older, we need to merge the
    //    new record.
    //
    val isNewerData = sierraTransformable.itemRecords.get(itemRecord.id) match {
      case Some(existing) =>
        itemRecord.modifiedDate.isAfter(existing.modifiedDate) ||
          itemRecord.modifiedDate == existing.modifiedDate
      case None => true
    }

    if (isNewerData) {
      val itemData = sierraTransformable.itemRecords + (itemRecord.id -> itemRecord)
      Some(sierraTransformable.copy(itemRecords = itemData))
    } else {
      None
    }
  }
}