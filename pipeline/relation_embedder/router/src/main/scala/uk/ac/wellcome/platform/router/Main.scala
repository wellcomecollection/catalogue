package uk.ac.wellcome.platform.router

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.Index
import com.typesafe.config.Config
import uk.ac.wellcome.elasticsearch.DenormalisedWorkIndexConfig
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.pipeline_storage.{ElasticIndexer, ElasticRetriever}
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val esClient = ElasticBuilder.buildElasticClient(config)
    val mergedIndex = Index(config.requireString("es.merged_index"))

    val denormalisedIndex = Index(config.requireString("es.denormalised_index"))
    new RouterWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      worksMsgSender = SNSBuilder
        .buildSNSMessageSender(
          config,
          namespace = "work-sender",
          subject = "Sent from the router"),
      pathsMsgSender = SNSBuilder
        .buildSNSMessageSender(
          config,
          namespace = "path-sender",
          subject = "Sent from the router"),
      workRetriever = new ElasticRetriever(esClient, mergedIndex),

      workIndexer = new ElasticIndexer(
        esClient,
        denormalisedIndex,
        DenormalisedWorkIndexConfig)
    )
  }
}
