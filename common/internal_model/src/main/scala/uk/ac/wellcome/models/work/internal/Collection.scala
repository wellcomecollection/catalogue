package uk.ac.wellcome.models.work.internal

/**
  * A CollectionPath represents the position of an individual work in a
  * collection hierarchy.
  */
case class CollectionPath(
  path: String,
  label: Option[String] = None,
) {

  lazy val tokens: List[String] =
    path.split("/").toList

  lazy val depth: Int =
    tokens.length

  def isDescendent(other: CollectionPath): Boolean =
    tokens.slice(0, other.depth) == other.tokens
}
