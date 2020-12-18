package uk.ac.wellcome.models.work.internal

/** An identifier received from one of the original sources */
case class SourceIdentifier(
  identifierType: IdentifierType,
  ontologyType: String,
  value: String
) {
  assert(!value.endsWith(" "))

  override def toString = s"${identifierType.id}/$value"
}
