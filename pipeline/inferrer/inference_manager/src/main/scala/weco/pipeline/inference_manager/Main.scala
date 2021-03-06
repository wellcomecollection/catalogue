package weco.pipeline.inference_manager

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.Config
import software.amazon.awssdk.services.sqs.model.Message
import weco.catalogue.internal_model.Implicits._
import weco.catalogue.internal_model.index.ImagesIndexConfig
import weco.typesafe.WellcomeTypesafeApp
import weco.messaging.typesafe.{SNSBuilder, SQSBuilder}
import weco.messaging.sns.NotificationMessage
import weco.typesafe.config.builders.AkkaBuilder
import weco.typesafe.config.builders.EnrichConfig._
import weco.elasticsearch.typesafe.ElasticBuilder
import weco.catalogue.internal_model.image.Image
import weco.catalogue.internal_model.image.ImageState.{Augmented, Initial}
import weco.pipeline.inference_manager.adapters.{
  AspectRatioInferrerAdapter,
  FeatureVectorInferrerAdapter,
  InferrerAdapter,
  PaletteInferrerAdapter
}
import weco.pipeline.inference_manager.models.DownloadedImage
import weco.pipeline.inference_manager.services.{
  ImageDownloader,
  InferenceManagerWorkerService
}
import weco.pipeline_storage.typesafe.{
  ElasticIndexerBuilder,
  ElasticSourceRetrieverBuilder,
  PipelineStorageStreamBuilder
}

object Main extends WellcomeTypesafeApp {
  runWithConfig { config: Config =>
    implicit val actorSystem: ActorSystem = AkkaBuilder.buildActorSystem()
    implicit val executionContext: ExecutionContext =
      AkkaBuilder.buildExecutionContext()

    val imageDownloader = ImageDownloader(
      config
        .getStringOption("shared.images-root")
        .getOrElse("/tmp")
    )

    val featureInferrerAdapter = new FeatureVectorInferrerAdapter(
      config.getString("inferrer.feature.host"),
      config.getInt("inferrer.feature.port")
    )
    val paletteInferrerAdapter = new PaletteInferrerAdapter(
      config.getString("inferrer.palette.host"),
      config.getInt("inferrer.palette.port")
    )
    val aspectRatioInferrerAdapter = new AspectRatioInferrerAdapter(
      config.getString("inferrer.aspectRatio.host"),
      config.getInt("inferrer.aspectRatio.port")
    )

    val inferrerClientFlow =
      Http().superPool[((DownloadedImage, InferrerAdapter), Message)]()

    val esClient = ElasticBuilder.buildElasticClient(config)

    val imageRetriever = ElasticSourceRetrieverBuilder.apply[Image[Initial]](
      config,
      esClient,
      namespace = "initial-images"
    )

    val imageIndexer = ElasticIndexerBuilder[Image[Augmented]](
      config,
      esClient,
      namespace = "augmented-images",
      indexConfig = ImagesIndexConfig.augmented
    )

    val pipelineStorageConfig = PipelineStorageStreamBuilder
      .buildPipelineStorageConfig(config)

    new InferenceManagerWorkerService(
      msgStream = SQSBuilder.buildSQSStream[NotificationMessage](config),
      msgSender = SNSBuilder.buildSNSMessageSender(
        config,
        subject = "Sent from the inference_manager"),
      imageRetriever = imageRetriever,
      imageIndexer = imageIndexer,
      pipelineStorageConfig = pipelineStorageConfig,
      imageDownloader = imageDownloader,
      inferrerAdapters = Set(
        featureInferrerAdapter,
        paletteInferrerAdapter,
        aspectRatioInferrerAdapter
      ),
      requestPool = inferrerClientFlow
    )
  }
}
