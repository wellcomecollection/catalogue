package weco.pipeline.transformer.sierra.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import weco.catalogue.internal_model.identifiers.{
  IdentifierType,
  SourceIdentifier
}
import weco.catalogue.source_model.generators.{
  MarcGenerators,
  SierraDataGenerators
}
import weco.catalogue.source_model.sierra.marc.{MarcSubfield, VarField}
import weco.catalogue.source_model.sierra.source.SierraMaterialType

class SierraIdentifiersTest
    extends AnyFunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  it("passes through the main identifier from the bib record") {
    val bibId = createSierraBibNumber
    val expectedIdentifiers = List(
      SourceIdentifier(
        identifierType = IdentifierType.SierraIdentifier,
        ontologyType = "Work",
        value = bibId.withoutCheckDigit
      )
    )
    SierraIdentifiers(bibId, createSierraBibData) shouldBe expectedIdentifiers
  }

  describe("finds ISBN identifiers from MARC 020 ǂa") {
    // This example is taken from b1754201
    val isbn10 = "159463078X"
    val isbn13 = "9781594630781"

    it("a single identifier") {
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "020", subfieldA = isbn10)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISBN,
          ontologyType = "Work",
          value = isbn10
        ))
    }

    it("multiple identifiers") {
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "020", subfieldA = isbn10),
          createVarFieldWith(marcTag = "020", subfieldA = isbn13)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISBN,
          ontologyType = "Work",
          value = isbn10
        ))
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISBN,
          ontologyType = "Work",
          value = isbn13
        ))
    }

    it("deduplicates identifiers") {
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "020", subfieldA = isbn10),
          createVarFieldWith(marcTag = "020", subfieldA = isbn10)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)

      val isbnIdentifiers = otherIdentifiers.filter {
        _.identifierType.id == "isbn"
      }
      isbnIdentifiers should have size 1
    }

    it("strips whitespace") {
      // Based on https://api.wellcomecollection.org/catalogue/v2/works/mhvnscj7?include=identifiers
      val isbn = "978-1479144075"

      val expectedIdentifier = SourceIdentifier(
        identifierType = IdentifierType.ISBN,
        ontologyType = "Work",
        value = isbn
      )

      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "020", subfieldA = s" $isbn")
        )
      )

      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(expectedIdentifier)
    }
  }

  describe("finds ISSN identifiers from MARC 022 ǂa") {
    it("a single identifier") {
      val issn = "0305-3342"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "022", subfieldA = issn)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISSN,
          ontologyType = "Work",
          value = issn
        ))
    }

    it("multiple identifiers") {
      val issn1 = "0305-3342"
      val issn2 = "0019-2422"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "022", subfieldA = issn1),
          createVarFieldWith(marcTag = "022", subfieldA = issn2)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISSN,
          ontologyType = "Work",
          value = issn1
        ))
      otherIdentifiers should contain(
        SourceIdentifier(
          identifierType = IdentifierType.ISSN,
          ontologyType = "Work",
          value = issn2
        ))
    }

    it("deduplicates identifiers") {
      val issn = "0305-3342"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "022", subfieldA = issn),
          createVarFieldWith(marcTag = "022", subfieldA = issn)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)

      val issnIdentifiers = otherIdentifiers.filter {
        _.identifierType.id == "issn"
      }
      issnIdentifiers should have size 1
    }

    it("strips whitespace") {
      // Based on https://api.wellcomecollection.org/catalogue/v2/works/t9wua9ys?include=identifiers
      val issn = "0945-7704"

      val expectedIdentifier = SourceIdentifier(
        identifierType = IdentifierType.ISSN,
        ontologyType = "Work",
        value = issn
      )

      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "022", subfieldA = s"$issn ")
        )
      )

      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(expectedIdentifier)
    }
  }

  describe("finds digcodes from MARC 759 ǂa") {
    it("a single identifier") {
      val digcode = "digrcs"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "759", subfieldA = digcode)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(createDigcodeIdentifier(digcode))
    }

    it("multiple identifiers") {
      // This example is based on b22474262
      val digcode1 = "digrcs"
      val digcode2 = "digukmhl"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "759", subfieldA = digcode1),
          createVarFieldWith(marcTag = "759", subfieldA = digcode2)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(createDigcodeIdentifier(digcode1))
      otherIdentifiers should contain(createDigcodeIdentifier(digcode2))
    }

    it("deduplicates identifiers") {
      val digcode = "digrcs"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "759", subfieldA = digcode),
          createVarFieldWith(marcTag = "759", subfieldA = digcode)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)

      val digcodeIdentifiers = otherIdentifiers.filter {
        _.identifierType.id == "wellcome-digcode"
      }
      digcodeIdentifiers should have size 1
    }

    it("skips values in MARC 759 which aren't digcodes") {
      val bibData = createSierraBibDataWith(
        varFields = List(
          // Although this starts with the special string `dig`, the lack
          // of any extra information makes it useless for identifying a
          // digitisation project!
          createVarFieldWith(marcTag = "759", subfieldA = "dig"),
          // digcodes have to start with the special string `dig`
          createVarFieldWith(marcTag = "759", subfieldA = "notadigcode")
        )
      )

      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)

      val digcodeIdentifiers = otherIdentifiers.filter {
        _.identifierType.id == "wellcome-digcode"
      }
      digcodeIdentifiers shouldBe empty
    }

    it("only captures the continuous alphabetic string starting `dig`") {
      // This example is based on b29500783
      val marcDigcode = "digmoh(Channel)"
      val parsedDigcode = "digmoh"

      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "759", subfieldA = marcDigcode)
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
      otherIdentifiers should contain(createDigcodeIdentifier(parsedDigcode))
    }

    it("deduplicates based on the actual digcode") {
      val digcode = "digmoh"
      val bibData = createSierraBibDataWith(
        varFields = List(
          createVarFieldWith(marcTag = "759", subfieldA = digcode),
          createVarFieldWith(marcTag = "759", subfieldA = s"$digcode(Channel)")
        )
      )
      val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)

      val digcodeIdentifiers = otherIdentifiers.filter {
        _.identifierType.id == "wellcome-digcode"
      }
      digcodeIdentifiers should have size 1
    }
  }

  it("finds the iconographic number from field 001 on visual collections") {
    val bibData = createSierraBibDataWith(
      materialType = Some(SierraMaterialType("r")),
      varFields = List(
        VarField(marcTag = Some("001"), content = Some("12345i"))
      )
    )

    val otherIdentifiers = SierraIdentifiers(createSierraBibNumber, bibData)
    otherIdentifiers should contain(
      SourceIdentifier(
        identifierType = IdentifierType.IconographicNumber,
        value = "12345i",
        ontologyType = "Work"
      )
    )
  }

  private def createVarFieldWith(marcTag: String, subfieldA: String): VarField =
    createVarFieldWith(
      marcTag = marcTag,
      subfields = List(
        MarcSubfield(tag = "a", content = subfieldA)
      )
    )
}
