package uk.ac.wellcome.platform.idminter

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import io.circe.Json
import uk.ac.wellcome.messaging.typesafe.BigMessagingBuilder
import uk.ac.wellcome.platform.idminter.config.builders.{IdentifiersTableBuilder, RDSBuilder}
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.IdentifiersTable
import uk.ac.wellcome.platform.idminter.services.IdMinterWorkerService
import uk.ac.wellcome.platform.idminter.steps.{IdEmbedder, IdentifierGenerator}
import uk.ac.wellcome.storage.streaming.CodecInstances._
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val materializer: ActorMaterializer =
      AkkaBuilder.buildActorMaterializer()

    val identifiersTableConfig = IdentifiersTableBuilder.buildConfig(config)

    val identifierGenerator = new IdentifierGenerator(
      identifiersDao = new IdentifiersDao(
        db = RDSBuilder.buildDB(config),
        identifiers = new IdentifiersTable(
          identifiersTableConfig = identifiersTableConfig
        )
      )
    )

    val idEmbedder = new IdEmbedder(
      identifierGenerator = identifierGenerator
    )

    new IdMinterWorkerService(
      idEmbedder = idEmbedder,
      messageSender = BigMessagingBuilder.buildBigMessageSender[Json](config),
      messageStream = BigMessagingBuilder.buildMessageStream[Json](config),
      rdsClientConfig = RDSBuilder.buildRDSClientConfig(config),
      identifiersTableConfig = identifiersTableConfig
    )
  }
}
