package weco.pipeline.transformer.miro.transformers

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import weco.catalogue.internal_model.work.{Concept, Subject}
import weco.pipeline.transformer.miro.generators.MiroRecordGenerators
import weco.pipeline.transformer.miro.source.MiroRecord

/** Tests that the Miro transformer extracts the "subjects" field correctly.
  *
  *  Although this transformation is currently a bit basic, the data we get
  *  from Miro will need cleaning before it's presented in the API (casing,
  *  names, etc.) -- these tests will become more complicated.
  */
class MiroSubjectsTest
    extends AnyFunSpec
    with Matchers
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("puts an empty subject list on records without keywords") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecord,
      expectedSubjectLabels = List()
    )
  }

  it("uses the image_keywords field if present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("Animals", "Arachnids", "Fruit"))
      ),
      expectedSubjectLabels = List("Animals", "Arachnids", "Fruit")
    )
  }

  it("uses the image_keywords_unauth field if present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywordsUnauth = Some(List(Some("Altruism"), Some("Mammals")))
      ),
      expectedSubjectLabels = List("Altruism", "Mammals")
    )
  }

  it("uses the image_keywords and image_keywords_unauth fields if both present") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("Humour")),
        keywordsUnauth = Some(List(Some("Marine creatures")))
      ),
      expectedSubjectLabels = List("Humour", "Marine creatures")
    )
  }

  it("normalises subject labels and concepts to sentence case") {
    transformRecordAndCheckSubjects(
      miroRecord = createMiroRecordWith(
        keywords = Some(List("humour", "comedic aspect")),
        keywordsUnauth = Some(List(Some("marine creatures")))
      ),
      expectedSubjectLabels =
        List("Humour", "Comedic aspect", "Marine creatures")
    )
  }

  private def transformRecordAndCheckSubjects(
    miroRecord: MiroRecord,
    expectedSubjectLabels: List[String]
  ): Assertion = {
    val transformedWork = transformWork(miroRecord)
    val expectedSubjects = expectedSubjectLabels.map { label =>
      Subject(
        label = label,
        concepts = List(Concept(label))
      )
    }
    transformedWork.data.subjects shouldBe expectedSubjects
  }
}
