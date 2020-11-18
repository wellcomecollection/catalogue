package uk.ac.wellcome.models.work.internal

import WorkState.Indexed

/** Holds relations for a particular work. This is a recursive data structure,
  * so related works can in turn hold their relations. An Option[List[_]] is
  * used to represent cases both where the work has no relations and where the
  * relations are not currently known.
  *
  * @param parts Children of the work
  * @param partOf Parents of the work
  * @param precededBy Siblings preceding the work
  * @param succeededBy Siblings following the work
  */
case class RelatedWorks(
  parts: Option[List[RelatedWork]] = None,
  partOf: Option[List[RelatedWork]] = None,
  precededBy: Option[List[RelatedWork]] = None,
  succeededBy: Option[List[RelatedWork]] = None,
)

case class RelatedWork(
  work: Work[Indexed],
  relatedWorks: RelatedWorks
)

object RelatedWorks {

  def unknown: RelatedWorks =
    RelatedWorks(
      parts = None,
      partOf = None,
      precededBy = None,
      succeededBy = None
    )

  def nil: RelatedWorks =
    RelatedWorks(
      parts = Some(Nil),
      partOf = Some(Nil),
      precededBy = Some(Nil),
      succeededBy = Some(Nil)
    )

  def partOf(relatedWork: RelatedWork,
             default: Option[List[RelatedWork]] = None): RelatedWorks =
    RelatedWorks(
      parts = default,
      partOf = Some(List(relatedWork)),
      precededBy = default,
      succeededBy = default
    )
}

object RelatedWork {

  def apply(work: Work[Indexed]): RelatedWork =
    RelatedWork(work, RelatedWorks.unknown)
}
