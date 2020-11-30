package uk.ac.wellcome.platform.merger.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Inspectors, OptionValues, PrivateMethodTester}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules.ImagesRule.FlatImageMergeRule
import uk.ac.wellcome.platform.merger.rules.WorkPredicates.WorkPredicate
import WorkState.Source
import uk.ac.wellcome.models.work.generators.{
  MetsWorkGenerators,
  MiroWorkGenerators,
  SierraWorkGenerators
}

class ImagesRuleTest
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
      val miroWorks = (1 to n).map(_ => miroSourceWork())
      val sierraWork = sierraDigitalSourceWork()
      val result = ImagesRule.merge(sierraWork, miroWorks.toList).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        miroWorks.map(_.data.images.head.locations)
    }

    it(
      "creates n images from a METS work containing n images, and a single Sierra picture work") {
      val n = 5
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = n)
      val sierraPictureWork = sierraSourceWork().format(Format.Pictures)
      val result = ImagesRule.merge(sierraPictureWork, List(metsWork)).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.images.map(_.locations)
    }

    it(
      "creates n images from a METS work containing n images, and a single Sierra ephemera work") {
      val n = 5
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = n)
      val sierraEphemeraWork = sierraSourceWork().format(Format.Ephemera)
      val result = ImagesRule.merge(sierraEphemeraWork, List(metsWork)).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.images.map(_.locations)
    }

    it(
      "creates n + m images from m Miro works, a METS work containing n images, and a single Sierra picture work") {
      val n = 3
      val m = 4
      val miroWorks = (1 to m).map(_ => miroSourceWork()).toList
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = n)
      val sierraPictureWork = sierraSourceWork().format(Format.Pictures)
      val result =
        ImagesRule.merge(sierraPictureWork, miroWorks :+ metsWork).data

      result should have length n + m
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.images.map(_.locations) ++
          miroWorks.map(_.data.images.head.locations)
    }

    it(
      "creates n + m images from m Miro works, a METS work containing n images, and a single Sierra ephemera work") {
      val n = 3
      val m = 4
      val miroWorks = (1 to m).map(_ => miroSourceWork()).toList
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = n)
      val sierraEphemeraWork = sierraSourceWork().format(Format.Ephemera)
      val result =
        ImagesRule.merge(sierraEphemeraWork, miroWorks :+ metsWork).data

      result should have length n + m
      result.map(_.locations) should contain theSameElementsAs
        metsWork.data.images.map(_.locations) ++
          miroWorks.map(_.data.images.head.locations)
    }

    it(
      "ignores METS images, but uses n Miro images, for a non-picture/ephemera Sierra work") {
      val n = 3
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = 3)
      val miroWorks = (1 to n).map(_ => miroSourceWork()).toList
      val sierraWork = sierraDigitalSourceWork()
      val result = ImagesRule.merge(sierraWork, miroWorks :+ metsWork).data

      result should have length n
      result.map(_.locations) should contain theSameElementsAs
        miroWorks.map(_.data.images.head.locations)
    }

    it(
      "does not use Miro images when a METS image is present for a digaids Sierra work") {
      val metsWork = createInvisibleMetsSourceWorkWith(numImages = 1)
      val miroWork = miroSourceWork()
      val sierraDigaidsWork = sierraSourceWork()
        .format(Format.Pictures)
        .otherIdentifiers(List(createDigcodeIdentifier("digaids")))
      val result =
        ImagesRule.merge(sierraDigaidsWork, List(miroWork, metsWork)).data

      result should have length 1
      result.map(_.locations) should contain theSameElementsAs metsWork.data.images
        .map(_.locations)
    }

    it(
      "will use Miro images for digaids Sierra works when no METS image is present") {
      val miroWork = miroSourceWork()
      val sierraDigaidsWork = sierraSourceWork()
        .format(Format.Pictures)
        .otherIdentifiers(List(createDigcodeIdentifier("digaids")))
      val result =
        ImagesRule.merge(sierraDigaidsWork, List(miroWork)).data

      result should have length 1
      result.map(_.locations) should contain theSameElementsAs miroWork.data.images
        .map(_.locations)
    }
  }

  describe("the flat image merging rule") {
    val testRule = new FlatImageMergeRule {
      override val isDefinedForTarget: WorkPredicate = _ => true
      override val isDefinedForSource: WorkPredicate = _ => true
    }

    it("creates images from every source") {
      val target = sierraDigitalSourceWork()
      val sources = (1 to 5).map(_ => miroSourceWork())
      testRule(target, sources).get should have length 5
    }
  }

  def createInvisibleMetsSourceWorkWith(
    numImages: Int): Work.Invisible[Source] = {
    val images =
      (1 to numImages).map { _ =>
        createSourceMetsImage
      }.toList

    metsSourceWork().images(images).invisible()
  }

}
