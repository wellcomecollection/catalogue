package uk.ac.wellcome.platform.matcher.matcher

import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import grizzled.slf4j.Logging

import uk.ac.wellcome.models.matcher.{
  MatchedIdentifiers,
  MatcherResult,
  WorkIdentifier,
  WorkNode
}
import uk.ac.wellcome.platform.matcher.exceptions.MatcherException
import uk.ac.wellcome.platform.matcher.models._
import uk.ac.wellcome.platform.matcher.storage.WorkGraphStore
import uk.ac.wellcome.platform.matcher.workgraph.WorkGraphUpdater
import uk.ac.wellcome.storage.locking.dynamo.DynamoLockingService
import uk.ac.wellcome.storage.locking.{
  FailedLockingServiceOp,
  FailedProcess,
  FailedUnlock
}

class WorkMatcher(
  workGraphStore: WorkGraphStore,
  lockingService: DynamoLockingService[Set[MatchedIdentifiers], Future])(
  implicit ec: ExecutionContext)
    extends Logging {

  type Out = Set[MatchedIdentifiers]

  def matchWork(links: WorkLinks): Future[MatcherResult] =
    doMatch(links).map(MatcherResult)

  private def doMatch(links: WorkLinks): Future[Out] =
    withLocks(links, links.ids) {
      for {
        beforeGraph <- workGraphStore.findAffectedWorks(links)
        afterGraph = WorkGraphUpdater.update(links, beforeGraph)
        _ <- withLocks(
          links,
          getGraphComponentIds(beforeGraph, afterGraph)) {
          // We are returning empty set here, as LockingService is tied to a
          // single `Out` type, here set to `Set[MatchedIdentifiers]`.
          // See issue here: https://github.com/wellcometrust/platform/issues/3873
          // TODO: Could we check to see if there are any updated nodes here?
          workGraphStore.put(afterGraph).map(_ => Set.empty)
        }
      } yield {
        convertToIdentifiersList(afterGraph)
      }
    }

  private def getGraphComponentIds(beforeGraph: WorkGraph,
                                   afterGraph: WorkGraph): Set[String] =
    beforeGraph.nodes.map(_.componentId) ++ afterGraph.nodes.map(_.componentId)

  private def withLocks(links: WorkLinks, ids: Set[String])(
    f: => Future[Out]): Future[Out] =
    lockingService
      .withLocks(ids)(f)
      .map {
        case Left(failure) =>
          debug(
            s"Locking failed while matching work ${links.workId}: $failure")
          throw MatcherException(failureToException(failure))
        case Right(out) => out
      }

  private def failureToException(failure: FailedLockingServiceOp): Throwable =
    failure match {
      case FailedUnlock(_, _, e) => e
      case FailedProcess(_, e)   => e
      case _                     => new RuntimeException(failure.toString)
    }

  private def convertToIdentifiersList(
    graph: WorkGraph): Set[MatchedIdentifiers] =
    groupBySetId(graph).map {
      case (_, workNodes: Set[WorkNode]) =>
        MatchedIdentifiers(workNodes.map(WorkIdentifier(_)))
    }.toSet

  private def groupBySetId(
    updatedGraph: WorkGraph): Map[String, Set[WorkNode]] =
    updatedGraph.nodes.groupBy(_.componentId)
}
