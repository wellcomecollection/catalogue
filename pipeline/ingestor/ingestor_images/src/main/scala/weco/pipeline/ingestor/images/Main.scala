package weco.pipeline.ingestor.images

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import com.typesafe.config.Config
import weco.typesafe.WellcomeTypesafeApp
import weco.typesafe.config.builders.AkkaBuilder
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.pipeline_storage.typesafe.ElasticSourceRetrieverBuilder
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.catalogue.internal_model.index.ImagesIndexConfig
import weco.messaging.sns.NotificationMessage
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.image.Image
import weco.catalogue.internal_model.image.ImageState.{Augmented, Indexed}
import weco.pipeline_storage.typesafe.{
  ElasticIndexerBuilder,
  ElasticSourceRetrieverBuilder,
  PipelineStorageStreamBuilder
}

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem =
      AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val msgStream =
      SQSBuilder.buildSQSStream[NotificationMessage](config)

    val client =
      ElasticBuilder
        .buildElasticClient(config, namespace = "pipeline_storage")

    val imageRetriever = ElasticSourceRetrieverBuilder[Image[Augmented]](
      config,
      client = client,
      namespace = "augmented-images")

    val imageIndexer =
      ElasticIndexerBuilder[Image[Indexed]](
        config,
        client = client,
        namespace = "indexed-images",
        indexConfig = ImagesIndexConfig.ingested
      )
    val msgSender = SNSBuilder
      .buildSNSMessageSender(config, subject = "Sent from the ingestor-images")

    val pipelineStream =
      PipelineStorageStreamBuilder.buildPipelineStorageStream(
        msgStream,
        imageIndexer,
        msgSender)(config)

    new ImageIngestorWorkerService(
      pipelineStream = pipelineStream,
      imageRetriever = imageRetriever,
      transform = ImageTransformer.deriveData
    )
  }
}
