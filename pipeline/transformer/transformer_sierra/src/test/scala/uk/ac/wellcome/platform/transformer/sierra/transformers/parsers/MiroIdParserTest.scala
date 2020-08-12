package uk.ac.wellcome.platform.transformer.sierra.transformers.parsers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.matchers.should.Matchers

class MiroIdParserTest extends AnyFunSpec with Matchers with MiroIdParser {

  it("parses miroIds") {
    forAll(
      Table(
        ("miroId", "parsedId"),
        ("V1234567", "V1234567"),
        ("V 1234567", "V1234567"),
        ("V 1", "V0000001"),
        ("V 12345", "V0012345"),
        ("L 12345", "L0012345"),
        ("V 12345 ER", "V0012345ER")
      )) { (miroId, expectedParsedId) =>
      parse089MiroId(miroId) shouldBe Some(expectedParsedId)
    }
  }

  it("does not parse invalid miroIds") {
    forAll(
      Table(
        "miroId",
        "",
        "11",
        "VV"
      )) { invalidMiroId =>
      parse089MiroId(invalidMiroId) shouldBe None
    }
  }
}
