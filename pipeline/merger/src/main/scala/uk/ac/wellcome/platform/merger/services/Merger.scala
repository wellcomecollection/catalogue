package uk.ac.wellcome.platform.merger.services

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules.MergerRule
import uk.ac.wellcome.platform.merger.rules.physicaldigital.SierraPhysicalDigitalMergeRule
import uk.ac.wellcome.platform.merger.rules.sierramets.SierraMetsMergerRule
import uk.ac.wellcome.platform.merger.rules.singlepagemiro.SinglePageMiroMergeRule

class Merger(rules: List[MergerRule]) {
  def merge(works: Seq[TransformedBaseWork]): Seq[BaseWork] = {
    rules.foldLeft(works: Seq[BaseWork])((works, rule) =>
      rule.mergeAndRedirectWorks(works))
  }
}

object PlatformMerger
    extends Merger(
      List(
        SierraPhysicalDigitalMergeRule,
        SinglePageMiroMergeRule,
        SierraMetsMergerRule))
