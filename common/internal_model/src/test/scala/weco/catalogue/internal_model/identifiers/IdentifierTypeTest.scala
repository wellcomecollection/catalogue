package weco.catalogue.internal_model.identifiers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class IdentifierTypeTest extends AnyFunSpec with Matchers {
  it("looks up an identifier type in the CSV") {
    IdentifierType("miro-image-number") shouldBe IdentifierType(
      id = "miro-image-number",
      label = "Miro image number"
    )
  }

  it("throws an error if looking up a non-existent identifier type") {
    val caught = intercept[IllegalArgumentException] {
      IdentifierType(platformId = "DoesNotExist")
    }
    caught.getMessage shouldBe "Unrecognised identifier type: [DoesNotExist]"
  }
}
