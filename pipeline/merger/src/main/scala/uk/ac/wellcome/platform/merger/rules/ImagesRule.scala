package uk.ac.wellcome.platform.merger.rules

import scala.Function.const
import cats.data.NonEmptyList

import uk.ac.wellcome.models.work.internal.{
  Identifiable,
  MergedImage,
  TransformedBaseWork,
  UnidentifiedWork,
  Unminted
}
import uk.ac.wellcome.platform.merger.models.FieldMergeResult
import uk.ac.wellcome.platform.merger.rules.WorkPredicates.{
  not,
  WorkPredicate,
  WorkPredicateOps
}

object ImagesRule extends FieldMergeRule {
  type FieldData = List[MergedImage[Unminted]]

  override def merge(
    target: UnidentifiedWork,
    sources: Seq[TransformedBaseWork] = Nil): FieldMergeResult[FieldData] =
    FieldMergeResult(
      data = sources match {
        case Nil =>
          getSingleMiroImage.applyOrElse(target, const(Nil))
        case _ :: _ =>
          getPictureImages(target, sources).getOrElse(Nil) ++
            getPairedMiroImages(target, sources).getOrElse(Nil)
      },
      sources = Nil
    )

  private lazy val getSingleMiroImage
    : PartialFunction[UnidentifiedWork, FieldData] = {
    case target if WorkPredicates.miroWork(target) =>
      target.data.images.map {
        _.mergeWith(
          parentWork = Identifiable(target.sourceIdentifier),
          fullText = createFulltext(List(target))
        )
      }
  }

  private lazy val getPictureImages = new FlatImageMergeRule {
    val isDefinedForTarget: WorkPredicate = WorkPredicates.sierraPicture
    val isDefinedForSource
      : WorkPredicate = WorkPredicates.metsWork or WorkPredicates.miroWork
  }

  private lazy val getPairedMiroImages = new FlatImageMergeRule {
    val isDefinedForTarget: WorkPredicate =
      WorkPredicates.sierraWork and not(WorkPredicates.sierraPicture)
    val isDefinedForSource: WorkPredicate = WorkPredicates.miroWork
  }

  trait FlatImageMergeRule extends PartialRule {
    final override def rule(target: UnidentifiedWork,
                            sources: NonEmptyList[TransformedBaseWork])
      : List[MergedImage[Unminted]] = {
      val works = sources.prepend(target).toList
      works flatMap {
        _.data.images.map {
          _.mergeWith(
            parentWork = Identifiable(target.sourceIdentifier),
            fullText = createFulltext(works)
          )
        }
      }
    }
  }

  private def createFulltext(works: Seq[TransformedBaseWork]): Option[String] =
    works
      .map(_.data)
      .flatMap { data =>
        List(
          data.title,
          data.description,
          data.physicalDescription,
          data.lettering
        )
      }
      .flatten match {
      case Nil    => None
      case fields => Some(fields.mkString(" "))
    }
}
