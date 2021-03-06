package weco.pipeline.calm_adapter

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.typesafe.config.builders.EnrichConfig._
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.json.JsonUtil._
import weco.catalogue.source_model.calm.CalmRecord
import weco.catalogue.source_model.config.SourceVHSBuilder
import weco.pipeline.calm_api_client.AkkaHttpCalmApiClient

object Main extends WellcomeTypesafeApp {

  runWithConfig { config =>
    implicit val ec: ExecutionContext =
      AkkaBuilder.buildExecutionContext()
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()

    val calmRetriever = new ApiCalmRetriever(
      apiClient = new AkkaHttpCalmApiClient(
        url = config.requireString("calm.api.url"),
        username = config.requireString("calm.api.username"),
        password = config.requireString("calm.api.password"),
      ),
      suppressedFields = config
        .requireString("calm.suppressedFields")
        .split(",")
        .toSet
    )

    new CalmAdapterWorkerService(
      SQSBuilder.buildSQSStream(config),
      SNSBuilder.buildSNSMessageSender(config, subject = "CALM adapter"),
      calmRetriever = calmRetriever,
      calmStore = new CalmStore(
        SourceVHSBuilder.build[CalmRecord](config)
      ),
    )
  }
}
