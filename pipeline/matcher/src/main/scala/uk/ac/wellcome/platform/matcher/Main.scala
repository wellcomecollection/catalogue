package uk.ac.wellcome.platform.matcher

import java.time.Duration
import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.scanamo.auto._
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.platform.matcher.matcher.WorkMatcher
import uk.ac.wellcome.platform.matcher.services.MatcherWorkerService
import uk.ac.wellcome.platform.matcher.storage.{WorkGraphStore, WorkNodeDao}
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.storage.locking.dynamo.{
  DynamoLockDao,
  DynamoLockDaoConfig,
  DynamoLockingService
}
import uk.ac.wellcome.storage.typesafe.DynamoBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import com.sksamuel.elastic4s.Index
import uk.ac.wellcome.platform.matcher.storage.elastic.ElasticWorkLinksRetriever

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val dynamoClient = DynamoBuilder.buildDynamoClient(config)

    val workGraphStore = new WorkGraphStore(
      workNodeDao = new WorkNodeDao(
        dynamoClient,
        DynamoBuilder.buildDynamoConfig(config)
      )
    )

    implicit val lockDao = new DynamoLockDao(
      dynamoClient,
      DynamoLockDaoConfig(
        DynamoBuilder.buildDynamoConfig(config, namespace = "locking.service"),
        Duration.ofSeconds(
          config
            .getIntOption("aws.locking.service.dynamo.timeout")
            .getOrElse(180)
            .toLong
        )
      )
    )

    val esClient = ElasticBuilder.buildElasticClient(config)

    val workMatcher = new WorkMatcher(workGraphStore, new DynamoLockingService)

    val workLinksRetriever =
      new ElasticWorkLinksRetriever(
        esClient,
        index = Index(config.requireString("es.index")))

    new MatcherWorkerService(
      workLinksRetriever = workLinksRetriever,
      msgStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      msgSender = SNSBuilder
        .buildSNSMessageSender(config, subject = "Sent from the matcher"),
      workMatcher = workMatcher
    )
  }
}
