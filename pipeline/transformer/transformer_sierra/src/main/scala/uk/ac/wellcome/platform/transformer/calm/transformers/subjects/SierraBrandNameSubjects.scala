package uk.ac.wellcome.platform.transformer.calm.transformers.subjects

import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.calm.source.VarField
import uk.ac.wellcome.platform.transformer.calm.transformers.SierraAgents

// Populate wwork:subject
//
// Use MARC field "652". This is not documented but is a custom field used to
// represent brand names
object SierraBrandNameSubjects
    extends SierraSubjectsTransformer
    with SierraAgents {

  val subjectVarFields = List("652")

  def getSubjectsFromVarFields(bibId: SierraBibNumber,
                               varFields: List[VarField]) =
    varFields
      .subfieldsWithTag("a")
      .contents
      .map(label => Subject(label, List(Concept(label))))
}
