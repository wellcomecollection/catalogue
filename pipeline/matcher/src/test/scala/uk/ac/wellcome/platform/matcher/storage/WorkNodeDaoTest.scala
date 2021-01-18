package uk.ac.wellcome.platform.matcher.storage

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import javax.naming.ConfigurationException
import org.scalatest.funspec.AnyFunSpec
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{
  BatchGetItemRequest,
  BatchWriteItemRequest,
  ProvisionedThroughputExceededException,
  QueryRequest
}
import uk.ac.wellcome.models.matcher.WorkNode
import uk.ac.wellcome.platform.matcher.exceptions.MatcherException
import uk.ac.wellcome.platform.matcher.fixtures.MatcherFixtures
import uk.ac.wellcome.storage.dynamo.DynamoConfig

import scala.language.higherKinds

class WorkNodeDaoTest
    extends AnyFunSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with MatcherFixtures {

  describe("Get from dynamo") {
    it("returns nothing if ids are not in dynamo") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          whenReady(workNodeDao.get(Set("Not-there"))) { workNodeSet =>
            workNodeSet shouldBe Set.empty
          }
        }
      }
    }

    it("returns WorkNodes which are stored in DynamoDB") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          val existingWorkA: WorkNode =
            WorkNode("A", 1, List("B"), "A+B")
          val existingWorkB: WorkNode = WorkNode("B", 0, Nil, "A+B")

          put(dynamoClient, table.name)(existingWorkA)
          put(dynamoClient, table.name)(existingWorkB)

          whenReady(workNodeDao.get(Set("A", "B"))) { work =>
            work shouldBe Set(existingWorkA, existingWorkB)
          }
        }
      }
    }

    it("returns an error if fetching from dynamo fails") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        val expectedException = new RuntimeException("FAILED!")

        when(dynamoClient.batchGetItem(any[BatchGetItemRequest]))
          .thenThrow(expectedException)

        val matcherGraphDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(matcherGraphDao.get(Set("A")).failed) { failedException =>
          failedException shouldBe expectedException
        }
      }
    }

    it(
      "returns a GracefulFailure if ProvisionedThroughputExceededException occurs during get from dynamo") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        when(dynamoClient.batchGetItem(any[BatchGetItemRequest]))
          .thenThrow(
            ProvisionedThroughputExceededException
              .builder()
              .message("BOOM!")
              .build())
        val workNodeDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(workNodeDao.get(Set("A")).failed) { failedException =>
          failedException shouldBe a[MatcherException]
        }
      }
    }
  }

  describe("Get by ComponentIds") {
    it("returns empty set if componentIds are not in dynamo") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          whenReady(workNodeDao.getByComponentIds(Set("Not-there"))) {
            workNodeSet =>
              workNodeSet shouldBe Set()
          }
        }
      }
    }

    it(
      "returns WorkNodes which are stored in DynamoDB for a given component id") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { matcherGraphDao =>
          val existingWorkNodeA: WorkNode = WorkNode("A", 1, List("B"), "A+B")
          val existingWorkNodeB: WorkNode = WorkNode("B", 0, Nil, "A+B")

          put(dynamoClient, table.name)(existingWorkNodeA)
          put(dynamoClient, table.name)(existingWorkNodeB)

          whenReady(matcherGraphDao.getByComponentIds(Set("A+B"))) {
            linkedWorks =>
              linkedWorks shouldBe Set(existingWorkNodeA, existingWorkNodeB)
          }
        }
      }
    }

    it(
      "returns an error if fetching from dynamo fails during a getByComponentIds") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        val expectedException = new RuntimeException("FAILED")
        when(dynamoClient.query(any[QueryRequest]))
          .thenThrow(expectedException)
        val workNodeDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(workNodeDao.getByComponentIds(Set("A+B")).failed) {
          failedException =>
            failedException shouldBe expectedException
        }
      }
    }

    it(
      "returns a GracefulFailure if ProvisionedThroughputExceededException occurs during a getByComponentIds") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        when(dynamoClient.query(any[QueryRequest]))
          .thenThrow(
            ProvisionedThroughputExceededException
              .builder()
              .message("BOOM!")
              .build())
        val workNodeDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(workNodeDao.getByComponentIds(Set("A+B")).failed) {
          failedException =>
            failedException shouldBe a[MatcherException]
        }
      }
    }

    it("returns an error if Scanamo fails during a getByComponentIds") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          case class BadRecord(id: String, componentId: String, version: String)
          val badRecord: BadRecord =
            BadRecord(id = "A", componentId = "A+B", version = "five")
          put(dynamoClient, table.name)(badRecord)

          whenReady(workNodeDao.getByComponentIds(Set("A+B")).failed) {
            _ shouldBe a[RuntimeException]
          }
        }
      }
    }
  }

  describe("Insert into dynamo") {
    it("puts a WorkNode") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          val work = WorkNode("A", 1, List("B"), "A+B")
          whenReady(workNodeDao.put(Set(work))) { _ =>
            val savedLinkedWork =
              get[WorkNode](dynamoClient, table.name)("id" === "A")
            savedLinkedWork shouldBe Some(Right(work))
          }
        }
      }
    }

    it("returns an error if Scanamo fails to put a record") {
      withWorkGraphTable { table =>
        withWorkNodeDao(table) { workNodeDao =>
          case class BadRecord(id: String, version: String)
          val badRecord: BadRecord = BadRecord(id = "A", version = "six")
          put(dynamoClient, table.name)(badRecord)

          whenReady(workNodeDao.get(Set("A")).failed) {
            _ shouldBe a[RuntimeException]
          }
        }
      }
    }

    it("returns an error if put to dynamo fails") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        val expectedException = new RuntimeException("FAILED")
        when(dynamoClient.batchWriteItem(any[BatchWriteItemRequest]))
          .thenThrow(expectedException)
        val workNodeDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(
          workNodeDao.put(Set(WorkNode("A", 1, List("B"), "A+B"))).failed) {
          _ shouldBe expectedException
        }
      }
    }

    it(
      "returns a GracefulFailure if ProvisionedThroughputExceededException occurs during put to dynamo") {
      withWorkGraphTable { table =>
        val dynamoClient = mock[DynamoDbClient]
        when(dynamoClient.batchWriteItem(any[BatchWriteItemRequest]))
          .thenThrow(
            ProvisionedThroughputExceededException
              .builder()
              .message("BOOM!")
              .build())
        val workNodeDao = new WorkNodeDao(
          dynamoClient,
          DynamoConfig(table.name, table.index)
        )

        whenReady(
          workNodeDao.put(Set(WorkNode("A", 1, List("B"), "A+B"))).failed) {
          _ shouldBe a[MatcherException]
        }
      }
    }

    it("cannot be instantiated if dynamoConfig.maybeIndex is None") {
      intercept[ConfigurationException] {
        new WorkNodeDao(
          dynamoClient,
          DynamoConfig("something", None)
        )
      }
    }
  }
}
