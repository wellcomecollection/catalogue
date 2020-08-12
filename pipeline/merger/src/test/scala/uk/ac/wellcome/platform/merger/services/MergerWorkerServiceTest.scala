package uk.ac.wellcome.platform.merger.services

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.messaging.fixtures.SQS.QueuePair
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, MatcherResult}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.platform.merger.fixtures.{
  MatcherResultFixture,
  WorkerServiceFixture
}
import uk.ac.wellcome.platform.merger.generators.WorksWithImagesGenerators

class MergerWorkerServiceTest
    extends AnyFunSpec
    with Matchers
    with Eventually
    with IntegrationPatience
    with WorksWithImagesGenerators
    with MatcherResultFixture
    with MockitoSugar
    with WorkerServiceFixture {

  it(
    "reads matcher result messages, retrieves the works from vhs and sends them to sns") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, metrics) =>
        val work1 = createUnidentifiedWork
        val work2 = createUnidentifiedWork
        val work3 = createUnidentifiedWork

        val matcherResult =
          matcherResultWith(Set(Set(work3), Set(work1, work2)))

        givenStoredInVhs(vhs, work1, work2, work3)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          senders.works
            .getMessages[BaseWork] should contain only (work1, work2, work3)

          metrics.incrementedCounts.length should be >= 1
          metrics.incrementedCounts.last should endWith("_success")
        }
    }
  }

  it("sends InvisibleWorks unmerged") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, metrics) =>
        val work = createUnidentifiedInvisibleWork

        val matcherResult = matcherResultWith(Set(Set(work)))

        givenStoredInVhs(vhs, work)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          senders.works.getMessages[BaseWork] should contain only work

          metrics.incrementedCounts.length shouldBe 1
          metrics.incrementedCounts.last should endWith("_success")
        }
    }
  }

  it("fails if the work is not in vhs") {
    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), senders, metrics) =>
        val work = createUnidentifiedWork

        val matcherResult = matcherResultWith(Set(Set(work)))

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, size = 1)

          senders.works.messages shouldBe empty

          metrics.incrementedCounts.length shouldBe 3
          metrics.incrementedCounts.last should endWith("_failure")
        }
    }
  }

  it("discards works with newer versions in vhs, sends along the others") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, _) =>
        val work = createUnidentifiedWork
        val olderWork = createUnidentifiedWork
        val newerWork = olderWork.copy(version = 2)

        val matcherResult = matcherResultWith(Set(Set(work, olderWork)))

        givenStoredInVhs(vhs, work, newerWork)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)
          val worksSent = senders.works.getMessages[BaseWork]
          worksSent should contain only work
        }
    }
  }

  it("discards works with version 0 and sends along the others") {
    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, metrics) =>
        val versionZeroWork = createUnidentifiedWorkWith(version = 0)
        val work = versionZeroWork
          .copy(version = 1)

        val matcherResult = matcherResultWith(Set(Set(work, versionZeroWork)))

        givenStoredInVhs(vhs, work)

        sendNotificationToSQS(
          queue = queue,
          message = matcherResult
        )

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = senders.works.getMessages[BaseWork]
          worksSent should contain only work

          metrics.incrementedCounts.length shouldBe 1
          metrics.incrementedCounts.last should endWith("_success")
        }
    }
  }

  it("sends a merged work and a redirected work to SQS") {
    val (sierraWorkWithMergeCandidate, sierraWorkMergeCandidate) =
      createSierraWorkWithDigitisedMergeCandidate

    val works = List(sierraWorkWithMergeCandidate, sierraWorkMergeCandidate)

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, _) =>
        givenStoredInVhs(vhs, works: _*)

        val matcherResult = MatcherResult(
          Set(
            MatchedIdentifiers(worksToWorkIdentifiers(works))
          )
        )

        sendNotificationToSQS(queue = queue, message = matcherResult)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = senders.works.getMessages[BaseWork].distinct
          worksSent should have size 2

          val redirectedWorks = worksSent.collect {
            case work: UnidentifiedRedirectedWork => work
          }
          val mergedWorks = worksSent.collect {
            case work: UnidentifiedWork => work
          }

          redirectedWorks should have size 1
          redirectedWorks.head.sourceIdentifier shouldBe sierraWorkMergeCandidate.sourceIdentifier
          redirectedWorks.head.redirect shouldBe IdentifiableRedirect(
            sierraWorkWithMergeCandidate.sourceIdentifier)

          mergedWorks should have size 1
          mergedWorks.head.sourceIdentifier shouldBe sierraWorkWithMergeCandidate.sourceIdentifier
        }
    }
  }

  it("sends an image, a merged work, and redirected works to SQS") {
    val (sierraWorkWithMergeCandidate, sierraWorkMergeCandidate) =
      createSierraWorkWithDigitisedMergeCandidate
    val miroWork = createMiroWork

    val works =
      List(sierraWorkWithMergeCandidate, sierraWorkMergeCandidate, miroWork)

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, _) =>
        givenStoredInVhs(vhs, works: _*)

        val matcherResult = MatcherResult(
          Set(
            MatchedIdentifiers(worksToWorkIdentifiers(works))
          )
        )

        sendNotificationToSQS(queue = queue, message = matcherResult)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = senders.works.getMessages[BaseWork].distinct
          worksSent should have size 3

          val imagesSent =
            senders.images
              .getMessages[MergedImage[Identifiable, Unminted]]
              .distinct
          imagesSent should have size 1

          val redirectedWorks = worksSent.collect {
            case work: UnidentifiedRedirectedWork => work
          }
          val mergedWorks = worksSent.collect {
            case work: UnidentifiedWork => work
          }

          redirectedWorks should have size 2
          redirectedWorks.map(_.sourceIdentifier) should contain only
            (sierraWorkMergeCandidate.sourceIdentifier, miroWork.sourceIdentifier)
          redirectedWorks.map(_.redirect) should contain only
            IdentifiableRedirect(sierraWorkWithMergeCandidate.sourceIdentifier)

          mergedWorks should have size 1
          mergedWorks.head.sourceIdentifier shouldBe sierraWorkWithMergeCandidate.sourceIdentifier

          imagesSent.head.id shouldBe miroWork.data.images.head.id
        }
    }
  }

  it("splits the received works into multiple merged works if required") {
    val (sierraWorkWithMergeCandidate1, sierraWorkMergeCandidate1) =
      createSierraWorkWithDigitisedMergeCandidate
    val (sierraWorkWithMergeCandidate2, sierraWorkMergeCandidate2) =
      createSierraWorkWithDigitisedMergeCandidate
    val workPair1 =
      List(sierraWorkWithMergeCandidate1, sierraWorkMergeCandidate1)
    val workPair2 =
      List(sierraWorkWithMergeCandidate2, sierraWorkMergeCandidate2)
    val works = workPair1 ++ workPair2

    withMergerWorkerServiceFixtures {
      case (vhs, QueuePair(queue, dlq), senders, _) =>
        givenStoredInVhs(vhs, works: _*)

        val matcherResult = MatcherResult(
          Set(
            MatchedIdentifiers(worksToWorkIdentifiers(workPair1)),
            MatchedIdentifiers(worksToWorkIdentifiers(workPair2))
          ))

        sendNotificationToSQS(queue = queue, message = matcherResult)

        eventually {
          assertQueueEmpty(queue)
          assertQueueEmpty(dlq)

          val worksSent = senders.works.getMessages[BaseWork]
          worksSent should have size 4

          val redirectedWorks = worksSent.collect {
            case work: UnidentifiedRedirectedWork => work
          }
          val mergedWorks = worksSent.collect {
            case work: UnidentifiedWork => work
          }

          redirectedWorks should have size 2
          mergedWorks should have size 2
        }
    }
  }

  it("fails if the message sent is not a matcher result") {
    withMergerWorkerServiceFixtures {
      case (_, QueuePair(queue, dlq), _, metrics) =>
        sendInvalidJSONto(queue)

        eventually {
          assertQueueEmpty(queue)
          assertQueueHasSize(dlq, 1)
          metrics.incrementedCounts.length shouldBe 3
          metrics.incrementedCounts.last should endWith("_recognisedFailure")
        }
    }
  }

  case class Senders(works: MemoryMessageSender, images: MemoryMessageSender)

  def withMergerWorkerServiceFixtures[R](
    testWith: TestWith[(VHS, QueuePair, Senders, MemoryMetrics[StandardUnit]),
                       R]): R =
    withVHS { vhs =>
      withLocalSqsQueuePair() {
        case queuePair @ QueuePair(queue, _) =>
          val workSender = new MemoryMessageSender()
          val imageSender = new MemoryMessageSender()

          val metrics = new MemoryMetrics[StandardUnit]

          withWorkerService(vhs, queue, workSender, imageSender, metrics) { _ =>
            testWith(
              (vhs, queuePair, Senders(workSender, imageSender), metrics)
            )
          }
      }
    }
}
