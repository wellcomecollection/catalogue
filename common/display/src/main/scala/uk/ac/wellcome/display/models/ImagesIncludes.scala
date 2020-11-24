package uk.ac.wellcome.display.models

sealed trait ImageInclude

object ImageInclude {
  case object VisuallySimilar extends ImageInclude
  case object WithSimilarFeatures extends ImageInclude
  case object WithSimilarColors extends ImageInclude
  case object SourceContributors extends ImageInclude
  case object SourceLanguages extends ImageInclude
}

sealed trait ImageIncludes {
  val visuallySimilar: Boolean
  val withSimilarFeatures: Boolean
  val withSimilarColors: Boolean
  val `source.contributors`: Boolean
  val `source.languages`: Boolean
}

case class SingleImageIncludes(
  visuallySimilar: Boolean,
  withSimilarFeatures: Boolean,
  withSimilarColors: Boolean,
  `source.contributors`: Boolean,
  `source.languages`: Boolean
) extends ImageIncludes

object SingleImageIncludes {
  import ImageInclude._

  def apply(includes: ImageInclude*): SingleImageIncludes =
    SingleImageIncludes(
      visuallySimilar = includes.contains(VisuallySimilar),
      withSimilarFeatures = includes.contains(WithSimilarFeatures),
      withSimilarColors = includes.contains(WithSimilarColors),
      `source.contributors` = includes.contains(SourceContributors),
      `source.languages` = includes.contains(SourceLanguages),
    )

  def none: SingleImageIncludes = SingleImageIncludes()
}

case class MultipleImagesIncludes(
  `source.contributors`: Boolean,
  `source.languages`: Boolean
) extends ImageIncludes {
  val visuallySimilar: Boolean = false
  val withSimilarFeatures: Boolean = false
  val withSimilarColors: Boolean = false
}

object MultipleImagesIncludes {
  import ImageInclude._

  def apply(includes: ImageInclude*): MultipleImagesIncludes =
    MultipleImagesIncludes(
      `source.contributors` = includes.contains(SourceContributors),
      `source.languages` = includes.contains(SourceLanguages),
    )

  def none: MultipleImagesIncludes = MultipleImagesIncludes()
}
