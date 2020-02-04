package uk.ac.wellcome.platform.transformer.calm.source

import io.circe.generic.extras.JsonKey

// Examples of varFields from the Sierra JSON:
//
//    {
//      "fieldTag": "b",
//      "content": "X111658"
//    }
//
//    {
//      "fieldTag": "a",
//      "marcTag": "949",
//      "ind1": "0",
//      "ind2": "0",
//      "subfields": [
//        {
//          "tag": "1",
//          "content": "STAX"
//        },
//        {
//          "tag": "2",
//          "content": "sepam"
//        }
//      ]
//    }
//
case class MarcSubfield(
  tag: String,
  content: String
)

case class VarField(
  content: Option[String] = None,
  marcTag: Option[String] = None,
  fieldTag: Option[String] = None,
  @JsonKey("ind1") indicator1: Option[String] = None,
  @JsonKey("ind2") indicator2: Option[String] = None,
  subfields: List[MarcSubfield] = Nil
)

// Examples of fixedFields from the Sierra JSON:
//
//    "98": {
//      "label": "PDATE",
//      "value": "2017-12-22T12:55:57Z"
//    },
//    "77": {
//      "label": "TOT RENEW",
//      "value": 12
//    }
//
case class FixedField(
  label: String,
  value: String
)
