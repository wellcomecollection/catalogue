package uk.ac.wellcome.platform.sierra_item_merger

import akka.actor.ActorSystem
import com.typesafe.config.Config
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.platform.sierra_item_merger.services.{
  SierraItemMergerUpdaterService,
  SierraItemMergerWorkerService
}
import uk.ac.wellcome.sierra_adapter.model.Implicits._
import uk.ac.wellcome.sierra_adapter.model.SierraTransformable
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import weco.catalogue.source_model.config.SourceVHSBuilder

import scala.concurrent.ExecutionContext

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val updaterService = new SierraItemMergerUpdaterService(
      sourceVHS = SourceVHSBuilder.build[SierraTransformable](config)
    )

    new SierraItemMergerWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      sierraItemMergerUpdaterService = updaterService,
      messageSender =
        SNSBuilder.buildSNSMessageSender(config, subject = "Sierra item merger")
    )
  }
}
