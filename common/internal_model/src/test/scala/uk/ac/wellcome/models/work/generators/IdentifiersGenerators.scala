package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.generators.RandomStrings
import uk.ac.wellcome.models.work.internal.{IdentifierType, SourceIdentifier}

import scala.util.Random

trait IdentifiersGenerators extends RandomStrings {
  def createCanonicalId: String = randomAlphanumeric(length = 10)

  def createSourceIdentifier: SourceIdentifier = createSourceIdentifierWith()

  def createSourceIdentifierWith(
    identifierType: IdentifierType = IdentifierType("miro-image-number"),
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"): SourceIdentifier =
    SourceIdentifier(
      identifierType = identifierType,
      value = value,
      ontologyType = ontologyType
    )

  def createSierraSystemSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-system-number"),
      value = value,
      ontologyType = ontologyType
    )

  def createMetsSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(identifierType = IdentifierType("mets"))

  def createSierraSystemSourceIdentifier: SourceIdentifier =
    createSierraSystemSourceIdentifierWith()

  def createSierraIdentifierSourceIdentifierWith(
    value: String = randomAlphanumeric(length = 10),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("sierra-identifier"),
      value = value,
      ontologyType = ontologyType
    )

  def createSierraIdentifierSourceIdentifier: SourceIdentifier =
    createSierraIdentifierSourceIdentifierWith()

  def createIsbnSourceIdentifier: SourceIdentifier =
    createSourceIdentifierWith(
      identifierType = IdentifierType("isbn")
    )

  private val miroIdPrefixes: Seq[Char] = Seq(
    'C', 'L', 'V', 'W', 'N', 'M', 'B', 'A', 'S', 'F', 'D'
  )

  private def randomCharFrom(list: Char*) =
    list(Random.nextInt(list.size))

  def randomMiroId(prefix: Char = randomCharFrom(miroIdPrefixes: _*),
                   length: Int = 8): String =
    s"%c%0${length - 1}d".format(
      prefix,
      Random.nextInt(math.pow(10, length - 1).toInt)
    )

  def createMiroSourceIdentifierWith(
    value: String = randomMiroId(),
    ontologyType: String = "Work"
  ): SourceIdentifier =
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      ontologyType = ontologyType,
      value = value
    )

  def createMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith()

  def createHistoricalLibraryMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith(
      value = randomMiroId(prefix = randomCharFrom('L', 'M'))
    )

  def createNonHistoricalLibraryMiroSourceIdentifier: SourceIdentifier =
    createMiroSourceIdentifierWith(
      value = randomMiroId(prefix = randomCharFrom(miroIdPrefixes.filter {
        case 'L' | 'M' => false
        case _         => true
      }: _*)))
}
