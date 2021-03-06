package weco.pipeline.merger.models

import weco.catalogue.internal_model.work.WorkState.Identified
import weco.catalogue.internal_model.work.Work
import weco.pipeline.merger.rules.WorkPredicates

object Sources {
  import WorkPredicates._

  def findFirstLinkedDigitisedSierraWorkFor(
    target: Work.Visible[Identified],
    sources: Seq[Work[Identified]]): Option[Work[Identified]] =
    if (physicalSierra(target)

        // Audiovisual works are catalogued as multiple bib records, one for the physical
        // format and another for the digitised version, where available.
        //
        // These bibs routinely contain different data, and we can't consider one of
        // them canonical, unlike other physical/digitised bib pairs.
        //
        // Longer term, this may change based if Collections Information change how
        // they catalogue AV works.
        //
        // See https://github.com/wellcomecollection/platform/issues/4876
        && !isAudiovisual(target)) {
      val digitisedLinkedIds = target.data.mergeCandidates
        .filter(_.reason.contains("Physical/digitised Sierra work"))
        .map(_.id.canonicalId)

      sources.find(source =>
        digitisedLinkedIds.contains(source.state.canonicalId))
    } else {
      None
    }
}
