package uk.ac.wellcome.platform.transformer.calm.generators

import uk.ac.wellcome.models.transformable.sierra.test.utils.SierraGenerators
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.platform.transformer.calm.source._
import uk.ac.wellcome.platform.transformer.calm.source.sierra.{
  SierraSourceLanguage,
  SierraSourceLocation
}

trait SierraDataGenerators extends IdentifiersGenerators with SierraGenerators {
  def createSierraBibDataWith(
    title: Option[String] = Some(randomAlphanumeric(25)),
    lang: Option[SierraSourceLanguage] = None,
    materialType: Option[SierraMaterialType] = None,
    locations: Option[List[SierraSourceLocation]] = None,
    varFields: List[VarField] = List()
  ): SierraBibData =
    SierraBibData(
      title = title,
      lang = lang,
      materialType = materialType,
      locations = locations,
      varFields = varFields
    )

  def createSierraBibData: SierraBibData = createSierraBibDataWith()

  def createSierraItemDataWith(
    deleted: Boolean = false,
    location: Option[SierraSourceLocation] = None
  ): SierraItemData =
    SierraItemData(
      deleted = deleted,
      location = location
    )

  def createSierraItemData: SierraItemData = createSierraItemDataWith()

  def createSierraMaterialTypeWith(code: String): SierraMaterialType =
    SierraMaterialType(code = code)
}
