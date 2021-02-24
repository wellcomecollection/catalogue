package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.internal.{DigitalLocation, Item}
import uk.ac.wellcome.models.work.internal.LocationType.OnlineResource
import uk.ac.wellcome.platform.transformer.sierra.generators.MarcGenerators
import uk.ac.wellcome.platform.transformer.sierra.source.{MarcSubfield, VarField}
import uk.ac.wellcome.sierra_adapter.model.SierraGenerators

class SierraElectronicResourcesTest
  extends AnyFunSpec
    with Matchers
    with MarcGenerators
    with SierraGenerators {
  it("returns an Item that uses the URL from 856 ǂu") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "856",
        subfields = List(
          MarcSubfield(tag = "u", content = "https://example.org/journal")
        )
      )
    )

    getElectronicResources(varFields) shouldBe List(
      Item(
        title = None,
        locations = List(
          DigitalLocation(
            url = "https://example.org/journal",
            locationType = OnlineResource
          )
        )
      )
    )
  }

  it("returns multiple Items if field 856 is repeated") {
    val varFields = List(
      createVarFieldWith(
        marcTag = "856",
        subfields = List(
          MarcSubfield(tag = "u", content = "https://example.org/journal")
        )
      ),
      createVarFieldWith(
        marcTag = "856",
        subfields = List(
          MarcSubfield(tag = "u", content = "https://example.org/another-journal")
        )
      )
    )

    getElectronicResources(varFields) shouldBe List(
      Item(
        title = None,
        locations = List(
          DigitalLocation(
            url = "https://example.org/journal",
            locationType = OnlineResource
          )
        )
      ),
      Item(
        title = None,
        locations = List(
          DigitalLocation(
            url = "https://example.org/another-journal",
            locationType = OnlineResource
          )
        )
      )
    )
  }

  describe("skips adding an item") {
    it("if there are no instances of field 856") {
      getElectronicResources(varFields = List()) shouldBe empty

      getElectronicResources(
        varFields = List(
          createVarFieldWith(marcTag = "855"),
          createVarFieldWith(marcTag = "857")
        )
      ) shouldBe empty
    }

    it("if 856 ǂu isn't a URL") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "856",
          subfields = List(
            MarcSubfield(tag = "u", content = "search for 'online journals'")
          )
        )
      )

      getElectronicResources(varFields) shouldBe empty
    }

    it("if 856 ǂu is repeated") {
      val varFields = List(
        createVarFieldWith(
          marcTag = "856",
          subfields = List(
            MarcSubfield(tag = "u", content = "https://example.org/journal"),
            MarcSubfield(tag = "u", content = "https://example.org/another-journal")
          )
        )
      )

      getElectronicResources(varFields) shouldBe empty
    }

    it("if 856 doesn't have an instance of ǂu") {
      // When we first added 856 on bibs, some of our catalogue records had
      // the URL in subfield ǂa.  Because it was only a small number of records
      // and it deviates from the MARC spec, we prefer not to handle it in
      // the transformer, and instead get it fixed in the catalogue.
      val varFields = List(
        createVarFieldWith(
          marcTag = "856",
          subfields = List(
            MarcSubfield(tag = "a", content = "https://example.org/journal")
          )
        )
      )

      getElectronicResources(varFields) shouldBe empty
    }
  }

  def getElectronicResources(varFields: List[VarField]): List[Item[_]] =
    SierraElectronicResources(
      bibId = createSierraBibNumber,
      varFields = varFields
    )
}
