package uk.ac.wellcome.models.work.generators

import java.time.Instant

import uk.ac.wellcome.models.work.internal._
import SourceWork._

trait ImageGenerators
    extends IdentifiersGenerators
    with ItemsGenerators
    with InstantGenerators
    with VectorGenerators
    with SierraWorkGenerators {
  def createUnmergedImageWith(
    locations: List[DigitalLocationDeprecated] = List(createDigitalLocation),
    version: Int = 1,
    identifierValue: String = randomAlphanumeric(10),
    identifierType: IdentifierType = IdentifierType("miro-image-number")
  ): UnmergedImage[DataState.Unidentified] =
    UnmergedImage(
      sourceIdentifier = createSourceIdentifierWith(
        identifierType = identifierType,
        value = identifierValue),
      version = version,
      locations = locations
    )

  def createUnmergedImage: UnmergedImage[DataState.Unidentified] =
    createUnmergedImageWith()

  def createUnmergedMiroImage = createUnmergedImageWith(
    locations = List(
      DigitalLocationDeprecated(
        url = "https://iiif.wellcomecollection.org/V01234.jpg",
        locationType = LocationType("iiif-image"),
        license = Some(License.CCBY)
      ))
  )

  def createUnmergedMetsImage = createUnmergedImageWith(
    locations = List(createDigitalLocation),
    identifierType = IdentifierType("mets-image")
  )

  def createIdentifiedMergedImageWith(
    imageId: IdState.Identified =
      IdState.Identified(createCanonicalId, createSourceIdentifier),
    locations: List[DigitalLocationDeprecated] = List(
      createDigitalLocationWith(locationType = createImageLocationType)),
    version: Int = 1,
    modifiedTime: Instant = instantInLast30Days,
    parentWork: Work.Visible[WorkState.Identified] = sierraIdentifiedWork(),
    redirectedWork: Option[Work[WorkState.Identified]] = Some(
      sierraIdentifiedWork())): MergedImage[DataState.Identified] =
    MergedImage[DataState.Identified](
      imageId,
      version,
      modifiedTime,
      locations,
      SourceWorks(parentWork.toSourceWork, redirectedWork.map(_.toSourceWork)))

  def createInferredData = {
    val features = randomVector(4096)
    val (features1, features2) = features.splitAt(features.size / 2)
    val lshEncodedFeatures = randomHash(32)
    val palette = randomColorVector()
    Some(
      InferredData(
        features1 = features1.toList,
        features2 = features2.toList,
        lshEncodedFeatures = lshEncodedFeatures.toList,
        palette = palette.toList
      )
    )
  }

  def createAugmentedImageWith(
    imageId: IdState.Identified = IdState.Identified(
      createCanonicalId,
      createSourceIdentifierWith(IdentifierType("miro-image-number"))),
    parentWork: Work.Visible[WorkState.Identified] = sierraIdentifiedWork(),
    redirectedWork: Option[Work.Visible[WorkState.Identified]] = Some(
      identifiedWork()),
    inferredData: Option[InferredData] = createInferredData,
    locations: List[DigitalLocationDeprecated] = List(createDigitalLocation),
    version: Int = 1,
    modifiedTime: Instant = instantInLast30Days,
  ) =
    createIdentifiedMergedImageWith(
      imageId,
      locations,
      version,
      modifiedTime,
      parentWork,
      redirectedWork
    ).augment(inferredData)

  def createAugmentedImage(): AugmentedImage = createAugmentedImageWith()

  def createLicensedImage(license: License): AugmentedImage =
    createAugmentedImageWith(
      locations = List(createDigitalLocationWith(license = Some(license)))
    )

  // Create a set of images with intersecting LSH lists to ensure
  // that similarity queries will return something. Returns them in order
  // of similarity.
  def createSimilarImages(n: Int,
                          similarFeatures: Boolean,
                          similarPalette: Boolean): Seq[AugmentedImage] = {
    val features = if (similarFeatures) {
      similarVectors(4096, n)
    } else { (1 to n).map(_ => randomVector(4096, maxR = 10.0f)) }
    val lshFeatures = if (similarFeatures) {
      similarHashes(32, n)
    } else {
      (1 to n).map(_ => randomHash(32))
    }
    val palettes = if (similarPalette) {
      similarColorVectors(n)
    } else {
      (1 to n).map(_ => randomColorVector())
    }
    (features, lshFeatures, palettes).zipped.map {
      case (f, l, p) =>
        createAugmentedImageWith(
          inferredData = Some(
            InferredData(
              features1 = f.slice(0, 2048).toList,
              features2 = f.slice(2048, 4096).toList,
              lshEncodedFeatures = l.toList,
              palette = p.toList
            )
          )
        )
    }
  }

  implicit class UnmergedImageIdOps(
    val image: UnmergedImage[DataState.Unidentified]) {
    def toIdentifiedWith(
      id: String = createCanonicalId): UnmergedImage[DataState.Identified] =
      UnmergedImage[DataState.Identified](
        id = IdState.Identified(
          canonicalId = id,
          sourceIdentifier = image.id.allSourceIdentifiers.head
        ),
        version = image.version,
        locations = image.locations
      )

    val toIdentified: UnmergedImage[DataState.Identified] =
      toIdentifiedWith()
  }
}
