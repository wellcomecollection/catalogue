package uk.ac.wellcome.platform.merger.rules.sierramets

import org.scalatest.{FunSpec, Inside, Matchers}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.model.MergedWork

class SierraMetsWorkPairMergerTest
    extends FunSpec
    with WorksGenerators
    with Matchers
    with Inside {

  val sierraWork = createSierraPhysicalWork
  val metsWork = createUnidentifiedInvisibleMetsWork

  val workPairMerger = new SierraMetsWorkPairMerger {}

  it("merges a physical Sierra and a Mets work") {
    val result = workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)

    val physicalItem = sierraWork.data.items.head

    val metsLocation = metsWork.data.items.head.locations.head
    val expectedItems = List(
      physicalItem.copy(
        locations = physicalItem.locations :+ metsLocation
      )
    )

    inside(result) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWork.version,
              sierraWork.sourceIdentifier,
              data,
              sierraWork.ontologyType,
              sierraWork.identifiedType),
            redirectedWorks)) =>
        data shouldBe sierraWork.data.copy(items = expectedItems)

        redirectedWorks.head shouldBe UnidentifiedRedirectedWork(
          sourceIdentifier = metsWork.sourceIdentifier,
          version = metsWork.version,
          redirect = IdentifiableRedirect(sierraWork.sourceIdentifier))
    }
  }

  it("does not duplicate digital locations for the same url as the METS one") {
    val digitalLocationNoLicense = createDigitalLocationWith(license = None)
    val digitalLocationWithLicense =
      digitalLocationNoLicense.copy(license = Some(License.CCBYNC))
    val physicalLocation = createPhysicalLocation
    val sierraItem = createIdentifiableItemWith(
      locations = List(physicalLocation, digitalLocationNoLicense))

    val expectedItems = List(
      sierraItem.copy(
        locations = List(physicalLocation, digitalLocationWithLicense))
    )

    val metsWork = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = List(
        createDigitalItemWith(locations = List(digitalLocationWithLicense)))
    )

    val sierraWorkWithDigitalLocation =
      createUnidentifiedSierraWorkWith(items = List(sierraItem))

    val result = workPairMerger.mergeAndRedirectWorkPair(
      sierraWorkWithDigitalLocation,
      metsWork)

    inside(result) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWorkWithDigitalLocation.version,
              sierraWorkWithDigitalLocation.sourceIdentifier,
              data,
              sierraWorkWithDigitalLocation.ontologyType,
              sierraWorkWithDigitalLocation.identifiedType),
            _)) =>
        data.items shouldBe expectedItems
    }

  }

  it("keeps digital locations with different urls from the METS one") {
    val sierraDigitalLocation = createDigitalLocation
    val metsDigitalLocation = createDigitalLocation
    val physicalLocation = createPhysicalLocation
    val sierraItem = createIdentifiableItemWith(
      locations = List(physicalLocation, sierraDigitalLocation))

    val expectedItems = List(
      sierraItem.copy(
        locations =
          List(physicalLocation, sierraDigitalLocation, metsDigitalLocation)
      )
    )

    val metsWork = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = List(createDigitalItemWith(locations = List(metsDigitalLocation)))
    )

    val sierraWork = createUnidentifiedSierraWorkWith(items = List(sierraItem))

    val result = workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)

    inside(result) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWork.version,
              sierraWork.sourceIdentifier,
              data,
              sierraWork.ontologyType,
              sierraWork.identifiedType),
            _)) =>
        data.items shouldBe expectedItems
    }
  }

  it("merges a digital Sierra and a Mets work") {
    val sierraWork = createSierraDigitalWork
    val result = workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)

    val digitalItem = sierraWork.data.items.head

    val metsLocation = metsWork.data.items.head.locations.head
    val expectedItems = List(
      digitalItem.copy(locations = digitalItem.locations :+ metsLocation)
    )

    inside(result) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWork.version,
              sierraWork.sourceIdentifier,
              data,
              sierraWork.ontologyType,
              sierraWork.identifiedType),
            redirectedWorks)) =>
        data shouldBe sierraWork.data.copy(items = expectedItems)

        redirectedWorks.head shouldBe UnidentifiedRedirectedWork(
          sourceIdentifier = metsWork.sourceIdentifier,
          version = metsWork.version,
          redirect = IdentifiableRedirect(sierraWork.sourceIdentifier))
    }
  }

  it("merges a digital Sierra and a Mets work using the Mets thumbnail") {
    val thumbnail = DigitalLocation(
      url = "https://path.to/thumbnail.jpg",
      locationType = LocationType("thumbnail-image"),
      license = Some(License.CCBY)
    )
    val sierraWork = createSierraDigitalWork
    val metsWork = createUnidentifiedInvisibleMetsWork withData { data =>
      data.copy(thumbnail = Some(thumbnail))
    }
    inside(workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWork.version,
              sierraWork.sourceIdentifier,
              data,
              sierraWork.ontologyType,
              sierraWork.identifiedType),
            redirectedWork)) =>
        data.thumbnail shouldBe Some(thumbnail)
    }
  }

  it("merges if the sierra work has more than one item") {
    val items = (1 to 3).map(_ => createPhysicalItem).toList
    val sierraWork = createUnidentifiedSierraWorkWith(items = items)

    inside(workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)) {
      case Some(
          MergedWork(
            UnidentifiedWork(
              sierraWork.version,
              sierraWork.sourceIdentifier,
              data,
              sierraWork.ontologyType,
              sierraWork.identifiedType),
            redirectedWorks)) =>
        data shouldBe sierraWork.data.copy(items = items ++ metsWork.data.items)

        redirectedWorks.head shouldBe UnidentifiedRedirectedWork(
          sourceIdentifier = metsWork.sourceIdentifier,
          version = metsWork.version,
          redirect = IdentifiableRedirect(sierraWork.sourceIdentifier))
    }
  }

  it("filters digital items for the same url as the METS item") {
    val digitalLocationCCBYNC =
      createDigitalLocationWith(license = Some(License.CCBYNC))
    val digitalLocationNoLicense = digitalLocationCCBYNC.copy(license = None)

    val physicalItem = createPhysicalItem
    val sierraWork = createUnidentifiedSierraWorkWith(items =
      List(physicalItem, createDigitalItemWith(List(digitalLocationNoLicense))))

    val metsItem = createDigitalItemWith(List(digitalLocationCCBYNC))
    val metsWork =
      createUnidentifiedInvisibleMetsWorkWith(items = List(metsItem))

    inside(workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWork)) {
      case Some(MergedWork(UnidentifiedWork(_, _, data, _, _), _)) =>
        data.items shouldBe List(physicalItem, metsItem)
    }
  }

  it("doesn't merge if the mets work has more than one item") {
    val metsWithMultipleItems = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = List(createDigitalItem, createDigitalItem)
    )

    workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWithMultipleItems) shouldBe None
  }

  it("doesn't merge if the sierra work has no items") {
    val sierraWorkNoItems = createUnidentifiedSierraWorkWith(items = Nil)

    workPairMerger.mergeAndRedirectWorkPair(sierraWorkNoItems, metsWork) shouldBe None
  }

  it("doesn't merge if the METS work has no items") {
    val metsWorkNoItems = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = Nil
    )

    workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsWorkNoItems) shouldBe None
  }

  it("doesn't merge if the METS work item has more than one location") {
    val metsMultipleLocations = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = List(
        createUnidentifiableItemWith(
          locations = List(createDigitalLocation, createDigitalLocation)))
    )

    workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsMultipleLocations) shouldBe None
  }

  it("doesn't merge if the METS work item has no locations") {
    val metsNoLocations = createUnidentifiedInvisibleWorkWith(
      sourceIdentifier = createMetsSourceIdentifier,
      items = List(createUnidentifiableItemWith(locations = Nil))
    )

    workPairMerger.mergeAndRedirectWorkPair(sierraWork, metsNoLocations) shouldBe None
  }

}
