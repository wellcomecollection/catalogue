package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.platform.transformer.sierra.source.SierraQueryOps
import weco.catalogue.source_model.sierra.SierraBibData

// Populate work:edition
//
// Field 250 is used for this. In the very rare case where multiple 250 fields
// are found, they are concatenated into a single string
object SierraEdition extends SierraDataTransformer with SierraQueryOps {

  type Output = Option[String]

  def apply(bibData: SierraBibData) =
    bibData
      .subfieldsWithTag("250" -> "a")
      .contentString(" ")
}
