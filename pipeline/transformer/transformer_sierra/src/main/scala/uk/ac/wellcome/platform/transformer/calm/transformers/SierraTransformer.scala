package uk.ac.wellcome.platform.transformer.calm.transformers

import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.platform.transformer.calm.source.SierraBibData

/**
  *  Trait for transforming incoming bib data to some output type
  */
trait SierraTransformer {

  type Output

  def apply(bibId: SierraBibNumber, bibData: SierraBibData): Output
}
