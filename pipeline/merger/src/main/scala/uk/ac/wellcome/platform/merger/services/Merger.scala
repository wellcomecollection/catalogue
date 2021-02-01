package uk.ac.wellcome.platform.merger.services

import cats.data.State
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.rules._
import uk.ac.wellcome.platform.merger.logging.MergerLogging
import uk.ac.wellcome.platform.merger.models.{
  FieldMergeResult,
  ImageDataWithSource,
  MergeResult,
  MergerOutcome
}
import WorkState.Identified

/*
 * The implementor of a Merger must provide:
 * - `findTarget`, which finds the target from the input works
 * - `createMergeResult`, a recipe for creating a merged target and a
 *   map with keys of works used in the merge and values of whether they
 *   should be redirected
 *
 * Calling `merge` with a list of works will return a new list of works including:
 * - the target work with all fields merged
 * - all redirected sources
 * - any other works untouched
 */
trait Merger extends MergerLogging {
  type MergeState = Map[Work[Identified], Boolean]

  protected def findTarget(
    works: Seq[Work[Identified]]): Option[Work.Visible[Identified]]

  protected def createMergeResult(
    target: Work.Visible[Identified],
    sources: Seq[Work[Identified]]): State[MergeState, MergeResult]

  private case class CategorisedWorks(
    target: Work.Visible[Identified],
    sources: Seq[Work[Identified]] = Nil,
    deleted: Seq[Work.Deleted[Identified]] = Nil
  ) {
    require(!sources.contains(target))
    require(deleted.intersect(sources).isEmpty)
  }

  private def categoriseWorks(works: Seq[Work[Identified]]): Option[CategorisedWorks] =
    works match {
      case List(unmatchedWork: Work.Visible[Identified]) =>
        Some(CategorisedWorks(target = unmatchedWork))
      case matchedWorks =>
        findTarget(matchedWorks).map { target =>
          CategorisedWorks(
            target = target,
            sources =
              matchedWorks
                .filterNot { _.isInstanceOf[Work.Deleted[Identified]]}
                .filterNot { _.sourceIdentifier == target.sourceIdentifier },
            deleted =
              matchedWorks.collect { case w: Work.Deleted[Identified] => w},
          )
        }
    }

  implicit class MergeResultAccumulation[T](val result: FieldMergeResult[T]) {
    def redirectSources: State[MergeState, T] = shouldRedirect(true)
    def retainSources: State[MergeState, T] = shouldRedirect(false)

    // If the state already contains a source, then don't change the existing `redirect` value
    // Otherwise, add the source with the current value.
    private def shouldRedirect(redirect: Boolean): State[MergeState, T] =
      State { prevState =>
        val nextState = result.sources.foldLeft(prevState) {
          case (state, source) if state.contains(source) => state
          case (state, source)                           => state + (source -> redirect)
        }
        (nextState, result.data)
      }
  }

  def merge(works: Seq[Work[Identified]]): MergerOutcome =
    categoriseWorks(works)
      .map {
        case CategorisedWorks(target, sources, deleted) =>
          assert((sources ++ deleted :+ target).toSet == works.toSet)

          logIntentions(target, sources)
          val (mergeResultSources, result) = createMergeResult(target, sources)
            .run(Map.empty)
            .value
          val redirectedSources = mergeResultSources.collect {
            case (source, true) => source
          }

          val remaining = sources.toSet -- redirectedSources
          val redirects = redirectedSources.map(redirectSourceToTarget(target))
          logResult(result, redirects.toList, remaining.toList)

          MergerOutcome(
            resultWorks = redirects.toList ++ remaining ++ deleted :+ result.mergedTarget,
            imagesWithSources = result.imageDataWithSources
          )
      }
      .getOrElse(MergerOutcome.passThrough(works))

  private def redirectSourceToTarget(target: Work.Visible[Identified])(
    source: Work[Identified]): Work.Redirected[Identified] =
    Work.Redirected[Identified](
      version = source.version,
      state = Identified(
        source.sourceIdentifier,
        source.state.canonicalId,
        source.state.modifiedTime),
      redirect =
        IdState.Identified(target.state.canonicalId, target.sourceIdentifier)
    )

  private def logIntentions(target: Work.Visible[Identified],
                            sources: Seq[Work[Identified]]): Unit =
    sources match {
      case Nil =>
        info(s"Processing ${describeWork(target)}")
      case _ =>
        info(s"Attempting to merge ${describeMergeSet(target, sources)}")
    }

  private def logResult(result: MergeResult,
                        redirects: Seq[Work[_]],
                        remaining: Seq[Work[_]]): Unit = {
    if (redirects.nonEmpty) {
      info(
        s"Merged ${describeMergeOutcome(result.mergedTarget, redirects, remaining)}")
    }
    if (result.imageDataWithSources.nonEmpty) {
      info(s"Created images ${describeImages(result.imageDataWithSources)}")
    }
  }
}

object Merger {
  // Parameter can't be `State` as that shadows the Cats type
  implicit class WorkMergingOps[StateT <: WorkState](work: Work[StateT]) {
    def mapData(
      f: WorkData[StateT#WorkDataState] => WorkData[StateT#WorkDataState]
    ): Work[StateT] =
      work match {
        case Work.Visible(version, data, state) =>
          Work.Visible(version, f(data), state)
        case Work.Invisible(version, data, state, reasons) =>
          Work.Invisible(version, f(data), state, reasons)
        case Work.Redirected(version, redirect, state) =>
          Work.Redirected(version, redirect, state)
        case Work.Deleted(version, data, state, reason) =>
          Work.Deleted(version, f(data), state, reason)
      }
  }
}

object PlatformMerger extends Merger {
  import SourceWork._
  import Merger.WorkMergingOps

  override def findTarget(
    works: Seq[Work[Identified]]): Option[Work.Visible[Identified]] =
    works
      .find(WorkPredicates.singlePhysicalItemCalmWork)
      .orElse(works.find(WorkPredicates.physicalSierra))
      .orElse(works.find(WorkPredicates.sierraWork)) match {
      case Some(target: Work.Visible[Identified]) => Some(target)
      case _                                      => None
    }

  override def createMergeResult(
    target: Work.Visible[Identified],
    sources: Seq[Work[Identified]]): State[MergeState, MergeResult] =
    if (sources.isEmpty)
      State.pure(
        MergeResult(
          mergedTarget = target,
          imageDataWithSources = standaloneImages(target).map { image =>
            ImageDataWithSource(
              image,
              SourceWorks(
                canonicalWork = target.toSourceWork,
                redirectedWork = None
              )
            )
          }
        )
      )
    else
      for {
        items <- ItemsRule(target, sources).redirectSources
        thumbnail <- ThumbnailRule(target, sources).redirectSources
        otherIdentifiers <- OtherIdentifiersRule(target, sources).redirectSources
        sourceImageData <- ImageDataRule(target, sources).redirectSources
        work = target.mapData { data =>
          data.copy[DataState.Identified](
            items = items,
            thumbnail = thumbnail,
            otherIdentifiers = otherIdentifiers,
            imageData = sourceImageData
          )
        }
      } yield
        MergeResult(
          mergedTarget = work,
          imageDataWithSources = sourceImageData.map { imageData =>
            ImageDataWithSource(
              imageData = imageData,
              source = SourceWorks(
                canonicalWork = work.toSourceWork,
                redirectedWork = sources
                  .find { _.data.imageData.contains(imageData) }
                  .map(_.toSourceWork)
              )
            )
          }
        )

  private def standaloneImages(
    target: Work.Visible[Identified]): List[ImageData[IdState.Identified]] =
    if (WorkPredicates.singleDigitalItemMiroWork(target)) target.data.imageData
    else Nil
}
