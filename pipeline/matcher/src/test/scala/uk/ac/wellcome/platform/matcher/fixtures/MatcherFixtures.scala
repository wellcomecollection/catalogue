package uk.ac.wellcome.platform.matcher.fixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import org.apache.commons.codec.digest.DigestUtils
import org.scanamo.{Scanamo, Table => ScanamoTable}
import org.scanamo.DynamoFormat
import org.scanamo.error.DynamoReadError
import org.scanamo.query.UniqueKey
import org.scanamo.semiauto._
import org.scanamo.time.JavaTimeFormats._
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.models.matcher.{MatchedIdentifiers, WorkNode}
import uk.ac.wellcome.platform.matcher.services.MatcherWorkerService
import uk.ac.wellcome.platform.matcher.storage.{WorkGraphStore, WorkNodeDao}
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.storage.dynamo._
import uk.ac.wellcome.storage.fixtures.DynamoFixtures.Table
import uk.ac.wellcome.storage.locking.dynamo.{
  DynamoLockDaoFixtures,
  DynamoLockingService,
  ExpiringLock
}
import WorkState.Identified
import uk.ac.wellcome.pipeline_storage.MemoryRetriever

import scala.collection.mutable

trait MatcherFixtures
    extends SQS
    with Akka
    with DynamoLockDaoFixtures
    with LocalWorkGraphDynamoDb {

  implicit val workNodeFormat: DynamoFormat[WorkNode] = deriveDynamoFormat
  implicit val lockFormat: DynamoFormat[ExpiringLock] = deriveDynamoFormat

  def withLockTable[R](testWith: TestWith[Table, R]): R =
    withSpecifiedLocalDynamoDbTable(createLockTable) { table =>
      testWith(table)
    }

  def withWorkGraphTable[R](testWith: TestWith[Table, R]): R =
    withSpecifiedLocalDynamoDbTable(createWorkGraphTable) { table =>
      testWith(table)
    }

  def withWorkerService[R](
    workRetriever: MemoryRetriever[Work[Identified]],
    queue: SQS.Queue,
    messageSender: MemoryMessageSender,
    graphTable: Table)(testWith: TestWith[MatcherWorkerService[String], R]): R =
    withLockTable { lockTable =>
      withWorkGraphStore(graphTable) { workGraphStore =>
        withWorkMatcher(workGraphStore, lockTable) { workMatcher =>
          withActorSystem { implicit actorSystem =>
            withSQSStream[NotificationMessage, R](queue) { msgStream =>
              val workerService =
                new MatcherWorkerService(
                  workRetriever = workRetriever,
                  msgStream,
                  messageSender,
                  workMatcher)
              workerService.run()
              testWith(workerService)
            }
          }
        }
      }
    }

  def withWorkerService[R](workRetriever: MemoryRetriever[Work[Identified]],
                           queue: SQS.Queue,
                           messageSender: MemoryMessageSender)(
    testWith: TestWith[MatcherWorkerService[String], R]): R =
    withWorkGraphTable { graphTable =>
      withWorkerService(workRetriever, queue, messageSender, graphTable) {
        service =>
          testWith(service)
      }
    }

  def withWorkMatcher[R](workGraphStore: WorkGraphStore, lockTable: Table)(
    testWith: TestWith[WorkMatcher, R]): R =
    withLockDao(dynamoClient, lockTable) { implicit lockDao =>
      val workMatcher = new WorkMatcher(
        workGraphStore = workGraphStore,
        lockingService = new DynamoLockingService
      )
      testWith(workMatcher)
    }

  def withWorkMatcherAndLockingService[R](
    workGraphStore: WorkGraphStore,
    lockingService: DynamoLockingService[Set[MatchedIdentifiers], Future])(
    testWith: TestWith[WorkMatcher, R]): R = {
    val workMatcher = new WorkMatcher(workGraphStore, lockingService)
    testWith(workMatcher)
  }

  def withWorkGraphStore[R](graphTable: Table)(
    testWith: TestWith[WorkGraphStore, R]): R = {
    withWorkNodeDao(graphTable) { workNodeDao =>
      val workGraphStore = new WorkGraphStore(workNodeDao)
      testWith(workGraphStore)
    }
  }

  def withWorkNodeDao[R](table: Table)(
    testWith: TestWith[WorkNodeDao, R]): R = {
    val workNodeDao = new WorkNodeDao(
      dynamoClient,
      DynamoConfig(table.name, table.index)
    )
    testWith(workNodeDao)
  }

  def sendWork(
    work: Work[Identified],
    retriever: MemoryRetriever[Work[Identified]],
    queue: SQS.Queue
  ): Any = {
    retriever.index ++= Map(work.id -> work)
    sendNotificationToSQS(queue, body = work.id)
  }

  def ciHash(str: String): String =
    DigestUtils.sha256Hex(str)

  def get[T](dynamoClient: AmazonDynamoDB, tableName: String)(
    key: UniqueKey[_])(
    implicit format: DynamoFormat[T]): Option[Either[DynamoReadError, T]] =
    Scanamo(dynamoClient).exec { ScanamoTable[T](tableName).get(key) }

  def put[T](dynamoClient: AmazonDynamoDB, tableName: String)(obj: T)(
    implicit format: DynamoFormat[T]) =
    Scanamo(dynamoClient).exec { ScanamoTable[T](tableName).put(obj) }

  def scan[T](dynamoClient: AmazonDynamoDB, tableName: String)(
    implicit format: DynamoFormat[T]): List[Either[DynamoReadError, T]] =
    Scanamo(dynamoClient).exec { ScanamoTable[T](tableName).scan() }

  def createRetriever: MemoryRetriever[Work[Identified]] =
    new MemoryRetriever[Work[Identified]](
      index = mutable.Map[String, Work[Identified]]()
    )
}
