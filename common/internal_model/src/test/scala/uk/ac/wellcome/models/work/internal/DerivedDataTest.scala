package uk.ac.wellcome.models.work.internal

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.generators.{ImageGenerators, WorkGenerators}

class DerivedDataTest
    extends AnyFunSpec
    with Matchers
    with WorkGenerators
    with ImageGenerators {

  describe("DerivedWorkData") {

    it("sets availableOnline = true if there is a digital location on an item") {
      val work = mergedWork().items(
        List(createDigitalItem, createIdentifiedPhysicalItem))
      val derivedWorkData = DerivedWorkData(work.data)

      derivedWorkData.availableOnline shouldBe true
    }

    it(
      "sets availableOnline = false if there is no digital location on any items") {
      val work = mergedWork().items(List(createIdentifiedPhysicalItem))
      val derivedWorkData = DerivedWorkData(work.data)

      derivedWorkData.availableOnline shouldBe false
    }

    it("handles empty items list") {
      val work = mergedWork().items(Nil)
      val derivedWorkData = DerivedWorkData(work.data)

      derivedWorkData.availableOnline shouldBe false
    }

  }

  describe("DerivedImageData") {

    it(
      "sets the thumbnail to the first iiif-image location it finds in locations") {
      val imageLocation = createImageLocation
      val image = createImageDataWith(locations =
        List(createManifestLocation, imageLocation)).toAugmentedImage
      val derivedImageData = DerivedImageData(image)

      derivedImageData.thumbnail shouldBe imageLocation
    }

    it("throws an error if there is no iiif-image location") {
      val image =
        createImageDataWith(locations = List(createManifestLocation)).toAugmentedImage

      assertThrows[RuntimeException] {
        DerivedImageData(image)
      }
    }
  }
}
