package uk.ac.wellcome.platform.merger.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.matcher.WorkIdentifier
import uk.ac.wellcome.models.work.generators.LegacyWorkGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.fixtures.LocalWorksVhs
import WorkState.Source

class RecorderPlaybackServiceTest
    extends AnyFunSpec
    with Matchers
    with ScalaFutures
    with LocalWorksVhs
    with LegacyWorkGenerators {

  it("fetches a single Work") {
    val work = createSourceWork

    withVHS { vhs =>
      givenStoredInVhs(vhs, work)

      whenReady(fetchAllWorks(vhs = vhs, work)) { result =>
        result shouldBe Seq(Some(work))
      }
    }
  }

  it("throws an error if asked to fetch a missing entry") {
    val work = createSourceWork

    withVHS { vhs =>
      whenReady(fetchAllWorks(vhs = vhs, work).failed) { result =>
        result shouldBe a[NoSuchElementException]
        result.getMessage shouldBe s"Work ${work.sourceIdentifier} is not in VHS!"
      }
    }
  }

  it("returns None if asked to fetch a Work without a version") {
    val work = createSourceWorkWith(version = 0)
    val workId = WorkIdentifier(work.sourceIdentifier.toString, None)

    withVHS { vhs =>
      val service = new RecorderPlaybackService(vhs)
      whenReady(service.fetchAllWorks(List(workId))) { result =>
        result shouldBe Seq(None)
      }
    }
  }

  it("returns None if the version in VHS has a higher version") {
    val work = createSourceWorkWith(version = 2)

    val workToStore = createSourceWorkWith(
      sourceIdentifier = work.sourceIdentifier,
      version = work.version + 1
    )

    withVHS { vhs =>
      givenStoredInVhs(vhs, workToStore)

      whenReady(fetchAllWorks(vhs = vhs, work)) { result =>
        result shouldBe Seq(None)
      }
    }
  }

  it("gets a mixture of works as appropriate") {
    val unchangedWorks = (1 to 3).map { _ =>
      createSourceWork
    }
    val outdatedWorks = (4 to 5).map { _ =>
      createSourceWork
    }
    val updatedWorks = outdatedWorks.map { work =>
      work.copy(version = work.version + 1)
    }

    val lookupWorks = (unchangedWorks ++ outdatedWorks).toList
    val storedWorks = (unchangedWorks ++ updatedWorks).toList

    withVHS { vhs =>
      givenStoredInVhs(vhs, storedWorks: _*)

      whenReady(fetchAllWorks(vhs = vhs, lookupWorks: _*)) { result =>
        result shouldBe (unchangedWorks.map { Some(_) } ++ (4 to 5).map { _ =>
          None
        })
      }
    }
  }

  private def fetchAllWorks(
    vhs: VHS,
    works: Work[Source]*): Future[Seq[Option[Work[Source]]]] = {
    val service = new RecorderPlaybackService(vhs)

    val workIdentifiers = works
      .map { w =>
        WorkIdentifier(w)
      }

    service.fetchAllWorks(workIdentifiers)
  }
}
