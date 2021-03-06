package weco.pipeline.transformer.miro.transformers

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.locations.DigitalLocation
import weco.pipeline.transformer.miro.generators.MiroRecordGenerators
import weco.pipeline.transformer.miro.source.MiroRecord

class MiroRecordTransformerCopyrightTest
    extends AnyFunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("has no credit line if there's not enough information") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecord,
      expectedCredit = None
    )
  }

  it("uses the image_credit_line field if present") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("Wellcome Collection")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("uses the image_credit_line in preference to image_source_code") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("Wellcome Collection"),
        sourceCode = Some("CAM")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("uses image_source_code if image_credit_line is empty") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = None,
        sourceCode = Some("CAM")
      ),
      expectedCredit = Some("Benedict Campbell")
    )
  }

  it("uses the uppercased version of the source_code if necessary") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("wel")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("tidies up the credit line if necessary") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        creditLine = Some("The Wellcome Library, London")
      ),
      expectedCredit = Some("Wellcome Collection")
    )
  }

  it("handles special characters in the contributor map") {
    transformRecordAndCheckCredit(
      miroRecord = createMiroRecordWith(
        sourceCode = Some("FEI")
      ),
      expectedCredit = Some("Fernán Federici")
    )
  }

  private def transformRecordAndCheckCredit(
    miroRecord: MiroRecord,
    expectedCredit: Option[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val location = transformedWork.data.items.head.locations.head
    location shouldBe a[DigitalLocation]
    location
      .asInstanceOf[DigitalLocation]
      .credit shouldBe expectedCredit
  }
}
