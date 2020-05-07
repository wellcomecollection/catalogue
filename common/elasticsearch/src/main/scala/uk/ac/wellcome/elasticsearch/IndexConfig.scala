package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl.{
  booleanField,
  dateField,
  intField,
  keywordField,
  objectField,
  properties,
  textField,
  tokenCountField
}
import com.sksamuel.elastic4s.requests.analysis.{
  Analysis,
  CustomAnalyzer,
  PathHierarchyTokenizer
}
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.requests.mappings.{
  FieldDefinition,
  MappingDefinition,
  ObjectField
}
import uk.ac.wellcome.elasticsearch.ImagesIndexConfig.englishTextField

trait IndexConfig {
  val mapping: MappingDefinition
  val analysis: Analysis

  // `textWithKeyword` and `keywordWithText` are slightly different in the semantics and their use case.
  // If the intended field type is keyword, but you would like to search it textually, use `keywordWithText` and
  // visa versa.

  // This encodes how someone would expect the field to work, but allow querying it in other ways.
  def textWithKeyword(name: String) =
    textField(name).fields(keywordField("keyword"))

  def keywordWithText(name: String) =
    keywordField(name).fields(textField("text"))

  def sourceIdentifierFields = Seq(
    keywordField("ontologyType"),
    objectField("identifierType").fields(
      label,
      keywordField("id"),
      keywordField("ontologyType")
    ),
    sourceIdentifierValue
  )

  def id(fieldName: String = "id") =
    objectField(fieldName).fields(
      keywordField("type"),
      canonicalId,
      objectField("sourceIdentifier").fields(sourceIdentifierFields),
      objectField("otherIdentifiers").fields(sourceIdentifierFields)
    )

  def location(fieldName: String = "locations") =
    objectField(fieldName).fields(
      keywordField("type"),
      keywordField("ontologyType"),
      objectField("locationType").fields(
        label,
        keywordField("id"),
        keywordField("ontologyType")
      ),
      label,
      textField("url"),
      textField("credit"),
      license,
      accessConditions
    )

  val accessConditions =
    objectField("accessConditions")
      .fields(
        englishTextField("terms"),
        textField("to"),
        objectField("status").fields(keywordField("type"))
      )

  val license = objectField("license").fields(
    keywordField("id")
  )

  val label = textWithKeyword("label")

  val sourceIdentifierValue = keywordWithText("value")

  val canonicalId = keywordWithText("canonicalId")
  val version = intField("version")
}

case object WorksIndexConfig extends IndexConfig {
  // Analysis
  val pathTokenizer = PathHierarchyTokenizer("path_hierarchy_tokenizer")
  val pathAnalyzer =
    CustomAnalyzer("path_hierarchy_analyzer", pathTokenizer.name, Nil, Nil)
  val analysis = Analysis(
    analyzers = List(
      pathAnalyzer
    ),
    tokenizers = List(pathTokenizer))

  val sourceIdentifier = objectField("sourceIdentifier")
    .fields(sourceIdentifierFields)

  val otherIdentifiers = objectField("otherIdentifiers")
    .fields(sourceIdentifierFields)

  val workType = objectField("workType")
    .fields(
      label,
      keywordField("ontologyType"),
      keywordField("id")
    )

  val notes = objectField("notes")
    .fields(
      keywordField("type"),
      englishTextField("content")
    )

  val period = Seq(
    label,
    id(),
    keywordField("ontologyType"),
    objectField("range").fields(
      label,
      dateField("from"),
      dateField("to"),
      booleanField("inferred")
    )
  )

  val place = Seq(
    label,
    id()
  )

  val concept = Seq(
    label,
    id(),
    keywordField("ontologyType"),
    keywordField("type")
  )

  val agent = Seq(
    label,
    id(),
    keywordField("type"),
    keywordField("prefix"),
    keywordField("numeration"),
    keywordField("ontologyType")
  )

  val rootConcept = concept ++ agent ++ period

  val subject: Seq[FieldDefinition] = Seq(
    id(),
    label,
    keywordField("ontologyType"),
    objectField("concepts").fields(rootConcept)
  )

  def subjects: ObjectField = objectField("subjects").fields(subject)

  def genre(fieldName: String) = objectField(fieldName).fields(
    label,
    keywordField("ontologyType"),
    objectField("concepts").fields(rootConcept)
  )

  def labelledTextField(fieldName: String) = objectField(fieldName).fields(
    label,
    keywordField("ontologyType")
  )

  def period(fieldName: String) = labelledTextField(fieldName)

  def items(fieldName: String) = objectField(fieldName).fields(
    id(),
    location(),
    englishTextField("title"),
    keywordField("ontologyType")
  )

  def englishTextField(name: String) =
    textField(name).fields(
      textField("english").analyzer("english")
    )

  val language = objectField("language").fields(
    label,
    keywordField("id"),
    keywordField("ontologyType")
  )

  val contributors = objectField("contributors").fields(
    id(),
    objectField("agent").fields(agent),
    objectField("roles").fields(
      label,
      keywordField("ontologyType")
    ),
    keywordField("ontologyType")
  )

  val production: ObjectField = objectField("production").fields(
    label,
    objectField("places").fields(place),
    objectField("agents").fields(agent),
    objectField("dates").fields(period),
    objectField("function").fields(concept),
    keywordField("ontologyType")
  )

  val mergeCandidates = objectField("mergeCandidates").fields(
    objectField("identifier").fields(sourceIdentifierFields),
    keywordField("reason")
  )

  val images = objectField("images").fields(
    id(),
    location("location"),
    version
  )

  val data: ObjectField =
    objectField("data").fields(
      otherIdentifiers,
      mergeCandidates,
      workType,
      textField("title").fields(
        keywordField("keyword"),
        textField("english").analyzer("english")
      ),
      englishTextField("alternativeTitles"),
      englishTextField("description"),
      englishTextField("physicalDescription"),
      englishTextField("lettering"),
      objectField("createdDate").fields(period),
      contributors,
      subjects,
      genre("genres"),
      items("items"),
      production,
      language,
      location("thumbnail"),
      textField("edition"),
      notes,
      intField("duration"),
      booleanField("merged"),
      objectField("collectionPath").fields(
        label,
        objectField("level").fields(keywordField("type")),
        textField("path")
          .copyTo("data.collectionPath.depth")
          .analyzer(pathAnalyzer.name)
          .fields(keywordField("keyword")),
        tokenCountField("depth").analyzer("standard")
      ),
      images
    )

  val fields: Seq[FieldDefinition with Product with Serializable] =
    Seq(
      canonicalId,
      keywordField("ontologyType"),
      version,
      sourceIdentifier,
      objectField("redirect")
        .fields(sourceIdentifier, canonicalId),
      keywordField("type"),
      data,
      // This is used for invisible works
      objectField("reason").fields(
        keywordField("type"),
        textField("info")
      )
    )

  val mapping = properties(fields).dynamic(DynamicMapping.Strict)

}
