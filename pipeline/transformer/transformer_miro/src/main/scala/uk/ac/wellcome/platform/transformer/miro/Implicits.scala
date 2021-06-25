package uk.ac.wellcome.platform.transformer.miro

import io.circe.generic.extras.semiauto._
import io.circe._

import weco.json.JsonUtil._
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

object Implicits {

  implicit val encoder: Encoder[MiroRecord] = deriveConfiguredEncoder
  implicit val decoder: Decoder[MiroRecord] = deriveConfiguredDecoder
}
