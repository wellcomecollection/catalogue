package uk.ac.wellcome.platform.merger.models

import java.time.Instant
import uk.ac.wellcome.models.work.internal._
import WorkState.{Identified, Merged}
import WorkFsm._

/*
 * MergerOutcome creates the final output of the merger:
 * all merged, redirected, and remaining works, as well as all merged images
 * the works/images getters must be provided with a modifiedTime to use for all
 * the output entities.
 */
case class MergerOutcome(resultWorks: Seq[Work[Identified]],
                         imagesWithSources: Seq[ImageDataWithSource]) {

  def mergedWorksWithTime(modifiedTime: Instant): Seq[Work[Merged]] =
    resultWorks.map(_.transition[Merged](modifiedTime))

  def mergedImagesWithTime(
    modifiedTime: Instant): Seq[Image[ImageState.Initial]] =
    imagesWithSources.map {
      case ImageDataWithSource(imageData, source) =>
        Image[ImageState.Initial](
          version = imageData.version,
          locations = imageData.locations,
          source = source,
          modifiedTime = modifiedTime,
          state = ImageState.Initial(
            sourceIdentifier = imageData.id.sourceIdentifier,
            canonicalId = imageData.id.canonicalId
          )
        )
    }
}

object MergerOutcome {

  def passThrough(works: Seq[Work[Identified]]): MergerOutcome =
    new MergerOutcome(works, Nil)
}
