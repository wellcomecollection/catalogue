package weco.pipeline.merger.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inspectors, OptionValues, PrivateMethodTester}
import weco.catalogue.internal_model.work.generators.SierraWorkGenerators
import weco.catalogue.internal_model.work.WorkState.Identified
import ImageDataRule.FlatImageMergeRule
import WorkPredicates.WorkPredicate
import weco.catalogue.internal_model.work.generators.{
  MetsWorkGenerators,
  MiroWorkGenerators,
  SierraWorkGenerators
}
import weco.catalogue.internal_model.work.{Format, Work}

class ImageDataRuleTest
    extends AnyFunSpec
    with Matchers
    with MiroWorkGenerators
    with SierraWorkGenerators
    with MetsWorkGenerators
    with PrivateMethodTester
    with OptionValues
    with Inspectors {
  describe("image creation rules") {
    it("creates n images from n Miro works and a single Sierra work") {
      val n = 3
      val miroWorks = (1 to n).map(_ => miroIdentifiedWork())
      val sierraWork = sierraDigitalIdentifiedWork()
      val result = ImageDataRule.merge(sierraWork, miroWorks.toList).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        miroWorks.map(_.data.imageData.head.locations)
    }

    it(
      "creates n images from a METS work containing n images, and a single Sierra picture work"
    ) {
      val n = 5
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = n)
      val sierraPictureWork = sierraIdentifiedWork().format(Format.Pictures)
      val result = ImageDataRule.merge(sierraPictureWork, List(metsWork)).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.imageData.map(_.locations)
    }

    it(
      "creates n images from a METS work containing n images, and a single Sierra ephemera work"
    ) {
      val n = 5
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = n)
      val sierraEphemeraWork = sierraIdentifiedWork().format(Format.Ephemera)
      val result = ImageDataRule.merge(sierraEphemeraWork, List(metsWork)).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.imageData.map(_.locations)
    }

    it(
      "creates n + m images from m Miro works, a METS work containing n images, and a single Sierra picture work"
    ) {
      val n = 3
      val m = 4
      val miroWorks = (1 to m).map(_ => miroIdentifiedWork()).toList
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = n)
      val sierraPictureWork = sierraIdentifiedWork().format(Format.Pictures)
      val result =
        ImageDataRule.merge(sierraPictureWork, miroWorks :+ metsWork).data

      result should have length n + m
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.imageData.map(_.locations) ++
          miroWorks.map(_.data.imageData.head.locations)
    }

    it(
      "creates n + m images from m Miro works, a METS work containing n images, and a single Sierra ephemera work"
    ) {
      val n = 3
      val m = 4
      val miroWorks = (1 to m).map(_ => miroIdentifiedWork()).toList
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = n)
      val sierraEphemeraWork = sierraIdentifiedWork().format(Format.Ephemera)
      val result =
        ImageDataRule.merge(sierraEphemeraWork, miroWorks :+ metsWork).data

      result should have length n + m
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.imageData.map(_.locations) ++
          miroWorks.map(_.data.imageData.head.locations)
    }

    it(
      "ignores METS images, but uses n Miro images, for a non-picture/ephemera Sierra work"
    ) {
      val n = 3
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = 3)
      val miroWorks = (1 to n).map(_ => miroIdentifiedWork()).toList
      val sierraWork = sierraDigitalIdentifiedWork()
      val result = ImageDataRule.merge(sierraWork, miroWorks :+ metsWork).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        miroWorks.map(_.data.imageData.head.locations)
    }

    it(
      "does not use Miro images when a METS image is present for a digaids Sierra work"
    ) {
      val metsWork = createInvisibleMetsIdentifiedWorkWith(numImages = 1)
      val miroWork = miroIdentifiedWork()
      val sierraDigaidsWork = sierraIdentifiedWork()
        .format(Format.Pictures)
        .otherIdentifiers(List(createDigcodeIdentifier("digaids")))
      val result =
        ImageDataRule.merge(sierraDigaidsWork, List(miroWork, metsWork)).data

      result should have length 1
      result.map(
        _.locations
      ) should contain theSameElementsAs metsWork.data.imageData
        .map(_.locations)
    }
  }

  describe("the flat image merging rule") {
    val testRule = new FlatImageMergeRule {
      override val isDefinedForTarget: WorkPredicate = _ => true
      override val isDefinedForSource: WorkPredicate = _ => true
    }

    it("creates images from every source") {
      val target = sierraDigitalIdentifiedWork()
      val sources = (1 to 5).map(_ => miroIdentifiedWork())
      testRule(target, sources).get should have length 5
    }
  }

  def createInvisibleMetsIdentifiedWorkWith(
    numImages: Int
  ): Work.Invisible[Identified] = {
    val images =
      (1 to numImages).map { _ =>
        createMetsImageData.toIdentified
      }.toList

    metsIdentifiedWork().imageData(images).invisible()
  }

}
