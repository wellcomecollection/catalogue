package uk.ac.wellcome.platform.merger

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.sksamuel.elastic4s.Index

import uk.ac.wellcome.bigmessaging.typesafe.{BigMessagingBuilder, VHSBuilder}
import uk.ac.wellcome.elasticsearch.typesafe.ElasticBuilder
import uk.ac.wellcome.elasticsearch.MergedWorkIndexConfig
import uk.ac.wellcome.pipeline_storage.ElasticIndexer
import uk.ac.wellcome.messaging.sns.NotificationMessage
import uk.ac.wellcome.messaging.typesafe.{SNSBuilder, SQSBuilder}
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.merger.services._
import uk.ac.wellcome.typesafe.WellcomeTypesafeApp
import uk.ac.wellcome.typesafe.config.builders.AkkaBuilder
import uk.ac.wellcome.typesafe.config.builders.EnrichConfig._
import WorkState.{Merged, Source}

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val playbackService = new RecorderPlaybackService(
      vhs = VHSBuilder.build[Work[Source]](config)
    )
    val mergerManager = new MergerManager(
      mergerRules = PlatformMerger
    )
    val workSender =
      SNSBuilder.buildSNSMessageSender(
        config,
        namespace = "work-sender",
        subject = "Sent by the merger"
      )
    val imageSender =
      BigMessagingBuilder
        .buildBigMessageSender(
          config.getConfig("image-sender").withFallback(config)
        )
    val elasticClient = ElasticBuilder.buildElasticClient(config)
    val index = Index(config.requireString("es.index"))

    new MergerWorkerService(
      sqsStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      playbackService = playbackService,
      mergerManager = mergerManager,
      workIndexer = new ElasticIndexer[Work[Merged]](
        elasticClient,
        index,
        MergedWorkIndexConfig),
      workSender = workSender,
      imageSender = imageSender
    )
  }
}
