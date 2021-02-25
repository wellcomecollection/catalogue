package weco.catalogue.sierra_adapter.models

import io.circe.generic.extras.semiauto._
import io.circe._
import uk.ac.wellcome.json.JsonUtil._

object Implicits {
  // Because the [[SierraTransformable.itemRecords]] field is keyed by
  // [[SierraItemNumber]] in our case class, but JSON only supports string
  // keys, we need to turn the ID into a string when storing as JSON.
  //
  // This is based on the "Custom key types" section of the Circe docs:
  // https://circe.github.io/circe/codecs/custom-codecs.html#custom-key-types
  //
  implicit val itemNumberEncoder: KeyEncoder[SierraItemNumber] =
    (key: SierraItemNumber) => key.withoutCheckDigit

  implicit val holdingsNumberEncoder: KeyEncoder[SierraHoldingsNumber] =
    (key: SierraHoldingsNumber) => key.withoutCheckDigit

  implicit val itemNumberDecoder: KeyDecoder[SierraItemNumber] =
    (key: String) => Some(SierraItemNumber(key))

  implicit val holdingsNumberDecoder: KeyDecoder[SierraHoldingsNumber] =
    (key: String) => Some(SierraHoldingsNumber(key))

  implicit val _dec01: Decoder[SierraTransformable] = deriveConfiguredDecoder
  implicit val _dec02: Decoder[SierraItemRecord] = deriveConfiguredDecoder
  implicit val _dec03: Decoder[SierraHoldingsRecord] = deriveConfiguredDecoder

  implicit val _enc01: Encoder[SierraTransformable] = deriveConfiguredEncoder
  implicit val _enc02: Encoder[SierraItemRecord] = deriveConfiguredEncoder
  implicit val _enc03: Encoder[SierraHoldingsRecord] = deriveConfiguredEncoder
}
