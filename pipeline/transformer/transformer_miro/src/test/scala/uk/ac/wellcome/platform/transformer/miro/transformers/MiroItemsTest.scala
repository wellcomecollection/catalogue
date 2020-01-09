package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators

class MiroItemsTest
    extends FunSpec
    with Matchers
    with IdentifiersGenerators
    with MiroRecordGenerators {
  val transformer = new MiroItems {}

  describe("getItems") {
    it("extracts an unidentifiable item") {
      transformer.getItems(
        createMiroRecordWith(
          sourceCode = Some("FDN"),
          useRestrictions = Some("CC-0"),
          imageNumber = "B0011308"
        )) shouldBe List(
        Unidentifiable(
          thing = Item(locations = List(DigitalLocation(
            "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
            LocationType("iiif-image"),
            Some(License.CC0),
            credit = Some("Ezra Feilden"))))))
    }
  }
}
