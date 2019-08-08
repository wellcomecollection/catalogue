package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.platform.transformer.sierra.source.{
  MarcSubfield,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.generators.{
  MarcGenerators,
  SierraDataGenerators
}

class SierraAlternativeTitlesTest
    extends FunSpec
    with Matchers
    with MarcGenerators
    with SierraDataGenerators {

  val (field240, field130, field246) = (
    createVarField("A", "240"),
    createVarField("B", "130"),
    createVarField("C", "246")
  )

  it("should extract an alternative title when there is 240") {
    val varFields = List(field240)
    getAlternativeTitles(varFields) shouldBe List("A")
  }

  it("should extract an alternative title when there is 130") {
    val varFields = List(field130)
    getAlternativeTitles(varFields) shouldBe List("B")
  }

  it("should extract an alternative title when there is 246") {
    val varFields = List(field246)
    getAlternativeTitles(varFields) shouldBe List("C")
  }

  it("should extract all alternative titles when multiple fields defined") {
    val varFields = List(field130, field240, field246)
    getAlternativeTitles(varFields) shouldBe List("A", "B", "C")
  }

  it("should extract all alternative titles when repeated fields") {
    val varFields = List(field240, createVarField("D", "240"))
    getAlternativeTitles(varFields) shouldBe List("A", "D")
  }

  it("should not extract any alternative titles when no 240 / 130 / 246") {
    val varFields = List(createVarField("X", "251"))
    getAlternativeTitles(varFields) shouldBe Nil
  }

  it("should not any extract alternative titles when incorrect contentTag") {
    val varFields = List(createVarField("X", "240", contentTag = "x"))
    getAlternativeTitles(varFields) shouldBe Nil
  }

  it("should not extract any alternative titles when 246 indicator2 is 6") {
    val varFields = List(createVarField("X", "246", indicator2 = "6"))
    getAlternativeTitles(varFields) shouldBe Nil
  }

  it("should still extract alternative titles when 240 / 130 indicator2 is 6") {
    val varFields = List(
      createVarField("A", "240", indicator2 = "6"),
      createVarField("B", "130", indicator2 = "6")
    )
    getAlternativeTitles(varFields) shouldBe List("A", "B")
  }

  val transformer = new SierraAlternativeTitles {}

  private def getAlternativeTitles(varFields: List[VarField]) =
    transformer getAlternativeTitles (createSierraBibDataWith(
      varFields = varFields))

  private def createVarField(
    content: String,
    tag: String,
    indicator2: String = "1",
    contentTag: String = "a"
  ) =
    createVarFieldWith(
      tag,
      indicator2,
      MarcSubfield(tag = contentTag, content = content) :: Nil)
}
