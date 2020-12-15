package uk.ac.wellcome.models.work.internal

sealed trait ImageSource[State <: DataState] {
  val id: State#Id
  val version: Int
}

case class SourceWorks[State <: DataState](
  canonicalWork: SourceWork[State],
  redirectedWork: Option[SourceWork[State]] = None
) extends ImageSource[State] {
  override val id = canonicalWork.id
  override val version =
    canonicalWork.version + redirectedWork.map(_.version).getOrElse(0)
}

case class SourceWork[State <: DataState](
  id: State#Id,
  data: WorkData[State],
  version: Int,
)

object SourceWork {

  implicit class SourceWorkToSourceWork(work: Work[WorkState.Source]) {

    def toSourceWork: SourceWork[DataState.Unidentified] =
      SourceWork[DataState.Unidentified](
        id = IdState.Identifiable(work.state.sourceIdentifier),
        data = work.data,
        version = work.version
      )
  }

  implicit class MergedWorkToSourceWork(work: Work[WorkState.Merged]) {

    def toSourceWork: SourceWork[DataState.Identified] =
      SourceWork[DataState.Identified](
        id = IdState
          .Identified(work.state.canonicalId, work.state.sourceIdentifier),
        data = work.data,
        version = work.version
      )
  }

  implicit class IdentifiedWorkToSourceWork(work: Work[WorkState.Identified]) {

    def toSourceWork: SourceWork[DataState.Identified] =
      SourceWork[DataState.Identified](
        id = IdState.Identified(
          sourceIdentifier = work.state.sourceIdentifier,
          canonicalId = work.state.canonicalId
        ),
        data = work.data,
        version = work.version
      )
  }
}
