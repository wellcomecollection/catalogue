package uk.ac.wellcome.models.work.internal

sealed trait BaseWork {
  val version: Int
  val sourceIdentifier: SourceIdentifier
}

sealed trait IdentifiedBaseWork extends BaseWork {
  val canonicalId: String
}

sealed trait TransformedBaseWork
    extends BaseWork
    with MultipleSourceIdentifiers {
  val data: WorkData[Unminted, Identifiable]
  val otherIdentifiers = data.otherIdentifiers
}

sealed trait InvisibleWork extends BaseWork

sealed trait RedirectedWork extends BaseWork {
  val redirect: Redirect
}

case class WorkData[Id <: IdState, ImageId <: WithSourceIdentifier](
  title: Option[String] = None,
  otherIdentifiers: List[SourceIdentifier] = Nil,
  mergeCandidates: List[MergeCandidate] = Nil,
  alternativeTitles: List[String] = Nil,
  workType: Option[WorkType] = None,
  description: Option[String] = None,
  physicalDescription: Option[String] = None,
  lettering: Option[String] = None,
  createdDate: Option[Period[Id]] = None,
  subjects: List[Subject[Id]] = Nil,
  genres: List[Genre[Id]] = Nil,
  contributors: List[Contributor[Id]] = Nil,
  thumbnail: Option[Location] = None,
  production: List[ProductionEvent[Id]] = Nil,
  language: Option[Language] = None,
  edition: Option[String] = None,
  notes: List[Note] = Nil,
  duration: Option[Int] = None,
  items: List[Item[Id]] = Nil,
  merged: Boolean = false,
  collectionPath: Option[CollectionPath] = None,
  images: List[UnmergedImage[ImageId]] = Nil
)

case class UnidentifiedWork(
  version: Int,
  sourceIdentifier: SourceIdentifier,
  data: WorkData[Unminted, Identifiable],
  ontologyType: String = "Work",
  identifiedType: String = classOf[IdentifiedWork].getSimpleName
) extends TransformedBaseWork {

  def withData(
    f: WorkData[Unminted, Identifiable] => WorkData[Unminted, Identifiable]) =
    this.copy(data = f(data))
}

case class IdentifiedWork(
  canonicalId: String,
  version: Int,
  sourceIdentifier: SourceIdentifier,
  data: WorkData[Minted, Identified],
  ontologyType: String = "Work"
) extends IdentifiedBaseWork
    with MultipleSourceIdentifiers {
  val otherIdentifiers = data.otherIdentifiers

  def withData(
    f: WorkData[Minted, Identified] => WorkData[Minted, Identified]) =
    this.copy(data = f(data))
}

case class UnidentifiedInvisibleWork(
  version: Int,
  sourceIdentifier: SourceIdentifier,
  data: WorkData[Unminted, Identifiable],
  reasons: List[InvisibilityReason],
  identifiedType: String = classOf[IdentifiedInvisibleWork].getSimpleName
) extends TransformedBaseWork
    with InvisibleWork {
  def withData(
    f: WorkData[Unminted, Identifiable] => WorkData[Unminted, Identifiable]) =
    this.copy(data = f(data))
}

case class IdentifiedInvisibleWork(
  canonicalId: String,
  version: Int,
  sourceIdentifier: SourceIdentifier,
  reasons: List[InvisibilityReason],
  data: WorkData[Minted, Identified]
) extends IdentifiedBaseWork
    with InvisibleWork {
  def withData(
    f: WorkData[Minted, Identified] => WorkData[Minted, Identified]) =
    this.copy(data = f(data))
}

case class UnidentifiedRedirectedWork(
  sourceIdentifier: SourceIdentifier,
  version: Int,
  redirect: IdentifiableRedirect,
  identifiedType: String = classOf[IdentifiedRedirectedWork].getSimpleName
) extends RedirectedWork

case class IdentifiedRedirectedWork(
  canonicalId: String,
  sourceIdentifier: SourceIdentifier,
  version: Int,
  redirect: IdentifiedRedirect
) extends IdentifiedBaseWork
    with RedirectedWork
