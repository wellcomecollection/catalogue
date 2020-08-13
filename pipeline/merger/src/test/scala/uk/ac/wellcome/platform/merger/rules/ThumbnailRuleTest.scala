package uk.ac.wellcome.platform.merger.rules
import org.scalatest.Inside
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.models.work.internal.{
  AccessCondition,
  AccessStatus,
  DigitalLocation,
  License,
  LocationType
}
import uk.ac.wellcome.platform.merger.models.FieldMergeResult

class ThumbnailRuleTest
    extends AnyFunSpec
    with Matchers
    with WorksGenerators
    with Inside {

  val physicalSierraWork = createSierraPhysicalWork

  val digitalSierraWork = createSierraDigitalWork

  val metsWork = createUnidentifiedWorkWith(
    sourceIdentifier = createMetsSourceIdentifier,
    items = List(createDigitalItem),
    thumbnail = Some(
      DigitalLocation(
        url = "mets.com/thumbnail.jpg",
        locationType = LocationType("thumbnail-image"),
        license = Some(License.CCBY)
      )
    )
  )

  val miroWorks = (0 to 3)
    .map { i =>
      f"V$i%04d"
    }
    .map { id =>
      createUnidentifiedWorkWith(
        sourceIdentifier = createMiroSourceIdentifierWith(id),
        thumbnail = Some(
          DigitalLocation(
            url = s"https://iiif.wellcomecollection.org/$id.jpg",
            locationType = LocationType("thumbnail-image"),
            license = Some(License.CCBY)
          )
        ),
        items = List(
          createUnidentifiableItemWith(
            locations = List(
              createDigitalLocationWith(
                locationType = createImageLocationType
              )
            )
          )
        )
      )
    }

  val restrictedDigitalWork =
    createUnidentifiedSierraWorkWith(
      items = List(
        createUnidentifiableItemWith(
          locations = List(
            createDigitalLocationWith(
              accessConditions = List(
                AccessCondition(
                  status = Some(AccessStatus.Restricted)
                )
              )
            )
          )
        )
      )
    )

  it(
    "chooses the METS thumbnail from a single-item digital METS work for a digital Sierra target") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe metsWork.data.thumbnail
    }
  }

  it(
    "chooses a Miro thumbnail if no METS works are available, for a digital Sierra target") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        miroWorks.map(_.data.thumbnail) should contain(thumbnail)
    }
  }

  it(
    "chooses a Miro thumbnail if no METS works are available, for a physical Sierra target") {
    inside(ThumbnailRule.merge(physicalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        miroWorks.map(_.data.thumbnail) should contain(thumbnail)
    }
  }

  it("chooses the Miro thumbnail from the work with the minimum ID") {
    inside(ThumbnailRule.merge(physicalSierraWork, miroWorks)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe defined
        thumbnail shouldBe miroWorks
          .minBy(_.sourceIdentifier.value)
          .data
          .thumbnail
    }
  }

  it("suppresses thumbnails when restricted access status") {
    inside(ThumbnailRule.merge(restrictedDigitalWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(thumbnail, _) =>
        thumbnail shouldBe None
    }
  }

  it("returns redirects of merged thumbnails") {
    inside(ThumbnailRule.merge(digitalSierraWork, miroWorks :+ metsWork)) {
      case FieldMergeResult(_, redirects) =>
        redirects should contain theSameElementsAs miroWorks :+ metsWork
    }
  }
}
