package uk.ac.wellcome.platform.inference_manager.adapters

import java.nio.{ByteBuffer, ByteOrder}
import java.util.Base64

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import uk.ac.wellcome.models.work.internal.{AugmentedImage, InferredData}
import uk.ac.wellcome.platform.inference_manager.models.{
  DownloadedImage,
  FeatureVectorInferrerResponse
}

import scala.util.{Success, Try}

/*
 * An InferrerAdapter is specific to the inferrer and the data that is being augmented.
 * Implementors must provide:
 * - The type of the inferrer response
 * - A Decoder for that response
 * - A function to create an HTTP request from the input data type
 * - A function to augment input data with a response, returning the output data type
 *
 * Additionally, the trait provides the logic for handling different HTTP response statuses
 */

trait InferrerAdapter[Input, Output] extends Logging {
  type InferrerResponse

  implicit val responseDecoder: Decoder[InferrerResponse]
  def createRequest(input: Input): HttpRequest
  def augmentInput(input: Input,
                   inferrerResponse: Option[InferrerResponse]): Output

  def parseResponse(response: HttpResponse)(
    implicit mat: Materializer): Future[Option[InferrerResponse]] =
    response.status match {
      case StatusCodes.OK =>
        Unmarshal(response.entity).to[Some[InferrerResponse]]
      case StatusCodes.BadRequest =>
        Future.failed(new Exception("Bad request"))
      case StatusCodes.NotFound =>
        Future.failed(new Exception("Entity not found"))
      case statusCode =>
        Future.failed(
          new Exception(s"Request failed with code ${statusCode.value}"))
    }
}

// The InferrerAdaptor for feature vectors, consuming MergedImages and
// augmenting them into AugmentedImages
object FeatureVectorInferrerAdapter
    extends InferrerAdapter[DownloadedImage, AugmentedImage] {
  type InferrerResponse = FeatureVectorInferrerResponse

  def createRequest(image: DownloadedImage): HttpRequest =
    HttpRequest(
      method = HttpMethods.GET,
      uri = Uri("/feature-vector/").withQuery(
        Uri.Query(
          "image_url" -> Uri
            .from(
              scheme = "file",
              path = image.pathString
            )
            .toString
        )
      )
    )

  def augmentInput(downloadedImage: DownloadedImage,
                   inferrerResponse: Option[InferrerResponse]): AugmentedImage =
    downloadedImage.image.augment {
      inferrerResponse flatMap {
        case FeatureVectorInferrerResponse(
            features_b64,
            lsh_encoded_features) =>
          val features = decodeBase64ToFloatList(features_b64)
          if (features.size == 4096) {
            val (features1, features2) = features.splitAt(features.size / 2)
            Some(
              InferredData(
                features1 = features1,
                features2 = features2,
                lshEncodedFeatures = lsh_encoded_features
              )
            )
          } else None
      }
    }

  private def decodeBase64ToFloatList(base64str: String): List[Float] = {
    // The JVM is big-endian whereas Python has encoded this with
    // little-endian ordering, so we need to manually set the order
    val buf = ByteBuffer
      .wrap(Base64.getDecoder.decode(base64str))
      .order(ByteOrder.LITTLE_ENDIAN)
    Stream
      .continually(Try(buf.getFloat))
      .takeWhile(_.isSuccess)
      .collect { case Success(f) => f }
      .toList
  }

  implicit val responseDecoder: Decoder[InferrerResponse] = deriveDecoder
}
