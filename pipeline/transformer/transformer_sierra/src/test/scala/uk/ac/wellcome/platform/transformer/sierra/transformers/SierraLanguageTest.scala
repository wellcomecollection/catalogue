package uk.ac.wellcome.platform.transformer.sierra.transformers

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.internal.Language
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLanguage
import uk.ac.wellcome.platform.transformer.sierra.generators.SierraDataGenerators

class SierraLanguageTest
    extends AnyFunSpec
    with Matchers
    with SierraDataGenerators {

  it("ignores records which don't have a lang field") {
    val bibData = createSierraBibDataWith(lang = None)
    SierraLanguage(bibData) shouldBe None
  }

  it("picks up the language from the lang field") {
    val bibData = createSierraBibDataWith(
      lang = Some(
        SierraSourceLanguage(
          code = "eng",
          name = "English"
        ))
    )

    SierraLanguage(bibData) shouldBe Some(
      Language(
        id = Some("eng"),
        label = "English"
      ))
  }
}
