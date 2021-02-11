package uk.ac.wellcome.calm_adapter

import scala.concurrent.ExecutionContext
import com.typesafe.config.Config
import akka.actor.ActorSystem
import akka.stream.Materializer
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.platform.calm_api_client.{
  CalmAkkaHttpClient,
  CalmApiClient,
  CalmHttpClient,
  CalmRecord,
}
import weco.catalogue.source_model.config.SourceVHSBuilder

object Main extends WellcomeTypesafeApp {

  runWithConfig { config =>
    implicit val ec: ExecutionContext =
      AkkaBuilder.buildExecutionContext()
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val materializer: Materializer =
      AkkaBuilder.buildMaterializer()
    implicit val httpClient: CalmHttpClient =
      new CalmAkkaHttpClient()

    new CalmAdapterWorkerService(
      SQSBuilder.buildSQSStream(config),
      SNSBuilder.buildSNSMessageSender(config, subject = "CALM adapter"),
      calmRetriever(config),
      calmStore = new CalmStore(
        SourceVHSBuilder.build[CalmRecord](config)
      ),
    )
  }

  def calmRetriever(config: Config)(implicit
                                    ec: ExecutionContext,
                                    materializer: Materializer,
                                    httpClient: CalmHttpClient) =
    new ApiCalmRetriever(
      apiClient = new CalmApiClient(
        url = config.requireString("calm.api.url"),
        username = config.requireString("calm.api.username"),
        password = config.requireString("calm.api.password"),
      ),
      suppressedFields = config
        .requireString("calm.suppressedFields")
        .split(",")
        .toSet
    )
}
