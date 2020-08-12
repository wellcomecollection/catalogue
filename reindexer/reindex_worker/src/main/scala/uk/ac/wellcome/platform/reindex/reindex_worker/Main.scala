package uk.ac.wellcome.platform.reindex.reindex_worker

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.platform.reindex.reindex_worker.config.ReindexJobConfigBuilder
import uk.ac.wellcome.platform.reindex.reindex_worker.dynamo.{
  BatchItemGetter,
  MaxRecordsScanner,
  ParallelScanner,
  ScanSpecScanner
}
import uk.ac.wellcome.platform.reindex.reindex_worker.services.{
  BulkMessageSender,
  RecordReader,
  ReindexWorkerService
}
import uk.ac.wellcome.storage.typesafe.DynamoBuilder
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val dynamoDBClient = DynamoBuilder.buildDynamoClient(config)
    val scanSpecScanner = new ScanSpecScanner(dynamoDBClient)

    val recordReader = new RecordReader(
      maxRecordsScanner = new MaxRecordsScanner(
        scanSpecScanner = scanSpecScanner
      ),
      parallelScanner = new ParallelScanner(
        scanSpecScanner = scanSpecScanner
      ),
      specificItemsGetter = new BatchItemGetter(
        dynamoDBClient
      )
    )

    val bulkMessageSender = new BulkMessageSender(
      underlying = SNSBuilder.buildSNSIndividualMessageSender(config)
    )

    new ReindexWorkerService(
      recordReader = recordReader,
      bulkMessageSender = bulkMessageSender,
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      reindexJobConfigMap =
        ReindexJobConfigBuilder.buildReindexJobConfigMap(config)
    )
  }
}
