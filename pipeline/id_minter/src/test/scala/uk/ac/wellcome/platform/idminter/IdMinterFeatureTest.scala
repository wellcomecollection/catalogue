package uk.ac.wellcome.platform.idminter

import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.idminter.fixtures.WorkerServiceFixture
import uk.ac.wellcome.models.Implicits._

class IdMinterFeatureTest
    extends AnyFunSpec
    with Matchers
    with IntegrationPatience
    with Eventually
    with WorkerServiceFixture
    with WorksGenerators {

  it("mints the same IDs where source identifiers match") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersDatabase { identifiersTableConfig =>
        withWorkerService(messageSender, queue, identifiersTableConfig) { _ =>
          eventuallyTableExists(identifiersTableConfig)
          val work = createUnidentifiedWork

          val messageCount = 5

          (1 to messageCount).foreach { _ =>
            sendMessage(queue = queue, obj = work)
          }

          eventually {
            val works = messageSender.getMessages[IdentifiedBaseWork]
            works.length shouldBe >=(messageCount)

            works.map(_.canonicalId).distinct should have size 1
            works.foreach { receivedWork =>
              receivedWork
                .asInstanceOf[IdentifiedWork]
                .sourceIdentifier shouldBe work.sourceIdentifier
              receivedWork
                .asInstanceOf[IdentifiedWork]
                .data
                .title shouldBe work.data.title
            }
          }
        }
      }
    }
  }

  it("mints an identifier for a UnidentifiedInvisibleWork") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersDatabase { identifiersTableConfig =>
        withWorkerService(messageSender, queue, identifiersTableConfig) { _ =>
          eventuallyTableExists(identifiersTableConfig)
          val work = createUnidentifiedInvisibleWork

          sendMessage(queue = queue, obj = work)

          eventually {
            val works = messageSender.getMessages[IdentifiedBaseWork]
            works.length shouldBe >=(1)

            val receivedWork = works.head
            val invisibleWork =
              receivedWork.asInstanceOf[IdentifiedInvisibleWork]
            invisibleWork.sourceIdentifier shouldBe work.sourceIdentifier
            invisibleWork.canonicalId shouldNot be(empty)
          }
        }
      }
    }
  }

  it("mints an identifier for a UnidentifiedRedirectedWork") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersDatabase { identifiersTableConfig =>
        withWorkerService(messageSender, queue, identifiersTableConfig) { _ =>
          eventuallyTableExists(identifiersTableConfig)

          val work = createUnidentifiedRedirectedWork

          sendMessage(queue = queue, obj = work)

          eventually {
            val works = messageSender.getMessages[IdentifiedBaseWork]
            works.length shouldBe >=(1)

            val receivedWork = works.head
            val redirectedWork =
              receivedWork.asInstanceOf[IdentifiedRedirectedWork]
            redirectedWork.sourceIdentifier shouldBe work.sourceIdentifier
            redirectedWork.canonicalId shouldNot be(empty)
            redirectedWork.redirect.canonicalId shouldNot be(empty)
          }
        }
      }
    }
  }

  it("continues if something fails processing a message") {
    val messageSender = new MemoryMessageSender()

    withLocalSqsQueue() { queue =>
      withIdentifiersDatabase { identifiersTableConfig =>
        withWorkerService(messageSender, queue, identifiersTableConfig) { _ =>
          sendInvalidJSONto(queue)

          val work = createUnidentifiedWork

          sendMessage(queue = queue, obj = work)

          eventually {
            messageSender.messages should not be empty

            assertMessageIsNotDeleted(queue)
          }
        }
      }
    }
  }

  private def assertMessageIsNotDeleted(queue: Queue): Unit = {
    // After a message is read, it stays invisible for 1 second and then it gets sent again.
    // So we wait for longer than the visibility timeout and then we assert that it has become
    // invisible again, which means that the id_minter picked it up again,
    // and so it wasn't deleted as part of the first run.
    // TODO Write this test using dead letter queues once https://github.com/adamw/elasticmq/issues/69 is closed
    Thread.sleep(2000)

    getQueueAttribute(
      queue,
      attributeName =
        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE) shouldBe "1"
  }
}
