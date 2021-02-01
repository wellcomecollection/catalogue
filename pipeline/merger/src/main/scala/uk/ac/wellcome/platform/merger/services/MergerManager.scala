package uk.ac.wellcome.platform.merger.services

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.models.MergerOutcome
import WorkState.Identified

class MergerManager(mergerRules: Merger) {

  /** Given a list of recorder work entries retrieved from VHS, and a
    * merging function, apply the function to these works.
    *
    * If we got an incomplete list of results from VHS (for example,
    * wrong versions), we skip the merge and return the original works.
    */
  def applyMerge(maybeWorks: Seq[Option[Work[Identified]]]): MergerOutcome = {
    val works = maybeWorks.flatten

    if (works.size == maybeWorks.size) {
      val result = mergerRules.merge(works)
      assert(result.resultWorks.size == works.size)
      result
    }
    else
      MergerOutcome.passThrough(works)
  }
}
