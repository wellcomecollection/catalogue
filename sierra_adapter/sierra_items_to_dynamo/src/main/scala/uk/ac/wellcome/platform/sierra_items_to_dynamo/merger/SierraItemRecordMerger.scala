package uk.ac.wellcome.platform.sierra_items_to_dynamo.merger

import uk.ac.wellcome.sierra_adapter.model.SierraItemRecord

object SierraItemRecordMerger {
  def mergeItems(existingRecord: SierraItemRecord,
                 updatedRecord: SierraItemRecord): Option[SierraItemRecord] = {
    if (existingRecord.modifiedDate.isBefore(updatedRecord.modifiedDate)) {
      Some(
        updatedRecord.copy(
          // Let's suppose we have
          //
          //    oldRecord = (linked = {1, 2, 3}, unlinked = {4, 5})
          //    newRecord = (linked = {3, 4})
          //
          // First we get a list of all the bibIds the oldRecord knew about, in any capacity:
          //
          //    addList(oldRecord.unlinkedBibIds, oldRecord.bibIds) = {1, 2, 3, 4, 5}
          //
          // Any bibIds in this list which are _not_ on the new record should be unlinked.
          //
          //    unlinkedBibIds
          //      = addList(oldRecord.unlinkedBibIds, oldRecord.bibIds) - newRecord.bibIds
          //      = (all bibIds on old record) - (current bibIds on new record)
          //      = {1, 2, 3, 4, 5} - {3, 4}
          //      = {1, 2, 5}
          //
          unlinkedBibIds = subList(
            addList(existingRecord.unlinkedBibIds, existingRecord.bibIds),
            updatedRecord.bibIds)
        )
      )
    } else {
      None
    }
  }

  private def addList[T](x: List[T], y: List[T]): List[T] = {
    (x.toSet ++ y.toSet).toList
  }

  private def subList[T](x: List[T], y: List[T]): List[T] = {
    (x.toSet -- y.toSet).toList
  }
}
