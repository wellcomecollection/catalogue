package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.work.internal.{DigitalLocation, _}

trait ItemsGenerators extends IdentifiersGenerators {
  def createIdentifiedItemWith(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    otherIdentifiers: List[SourceIdentifier] = Nil,
    locations: List[Location] = List(defaultLocation),
    title: Option[String] = None,
  ): Identified[Item] =
    Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      thing = Item(locations = locations, title = title)
    )

  def createIdentifiedItem: Identified[Item] = createIdentifiedItemWith()

  def createIdentifiedItems(count: Int): List[Identified[Item]] =
    (1 to count).map { _ =>
      createIdentifiedItem
    }.toList

  def createIdentifiableItemWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    locations: List[Location] = List(defaultLocation)
  ): Identifiable[Item] =
    Identifiable(
      sourceIdentifier = sourceIdentifier,
      thing = Item(locations = locations)
    )

  def createUnidentifiableItemWith(
    locations: List[Location] = List(defaultLocation)) =
    Unidentifiable(
      thing = Item(locations = locations)
    )

  def createPhysicalLocation = createPhysicalLocationWith()

  def createPhysicalLocationWith(locationType: LocationType =
                                   createStoresLocationType,
                                 label: String = "locationLabel") =
    PhysicalLocation(locationType, label)

  def createDigitalLocation = createDigitalLocationWith()

  def createDigitalLocationWith(
    locationType: LocationType = createPresentationLocationType,
    url: String = defaultLocationUrl,
    license: Option[License] = Some(License.CCBY)) = DigitalLocation(
    locationType = locationType,
    url = url,
    license = license
  )

  def createImageLocationType = LocationType("iiif-image")

  def createPresentationLocationType = LocationType("iiif-presentation")

  def createStoresLocationType = LocationType("sgmed")

  def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))

  def createDigitalItemWith(locations: List[Location]): Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = locations)

  def createDigitalItemWith(license: Option[License]): Unidentifiable[Item] =
    createUnidentifiableItemWith(
      locations = List(createDigitalLocationWith(license = license))
    )

  private def defaultLocation = createDigitalLocationWith()

  private def defaultLocationUrl =
    s"https://iiif.wellcomecollection.org/image/${randomAlphanumeric(3)}.jpg/info.json"
}
