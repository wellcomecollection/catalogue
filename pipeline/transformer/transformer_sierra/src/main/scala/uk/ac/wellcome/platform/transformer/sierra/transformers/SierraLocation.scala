package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraItemData,
  SierraQueryOps,
  VarField
}
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLocation

trait SierraLocation extends SierraQueryOps {

  def getPhysicalLocation(
    itemData: SierraItemData,
    bibData: SierraBibData): Option[PhysicalLocationDeprecated] =
    itemData.location.flatMap {
      // We've seen records where the "location" field is populated in
      // the JSON, but the code and name are both empty strings or "none".
      // We can't do anything useful with this, so don't return a location.
      case SierraSourceLocation("", "")         => None
      case SierraSourceLocation("none", "none") => None
      case SierraSourceLocation(code, name) =>
        Some(
          PhysicalLocationDeprecated(
            locationType = LocationType(code),
            accessConditions = getAccessConditions(bibData),
            label = name
          )
        )
    }

  def getDigitalLocation(identifier: String): DigitalLocationDeprecated = {
    // This is a defensive check, it may not be needed since an identifier should always be present.
    if (!identifier.isEmpty) {
      DigitalLocationDeprecated(
        url = s"https://wellcomelibrary.org/iiif/$identifier/manifest",
        license = None,
        locationType = LocationType("iiif-presentation")
      )
    } else {
      throw SierraTransformerException(
        "id required by DigitalLocation has not been provided")
    }
  }

  private def getAccessConditions(
    bibData: SierraBibData): List[AccessCondition] =
    bibData
      .varfieldsWithTag("506")
      .map { varfield =>
        AccessCondition(
          status = getAccessStatus(varfield),
          terms = varfield.nonrepeatableSubfieldWithTag("a").map { _.content },
          to = varfield.subfieldsWithTag("g").contents.headOption
        )
      }
      .filter {
        case AccessCondition(None, None, None) => false
        case _                                 => true
      }

  // Get an AccessStatus that draws from our list of types.
  //
  // Rules:
  //  - if the first indicator is 0, then there are no restrictions
  //  - look in subfield ǂf for the standardised terminology
  //
  // See https://www.loc.gov/marc/bibliographic/bd506.html
  private def getAccessStatus(varfield: VarField): Option[AccessStatus] =
    if (varfield.indicator1.contains("0"))
      Some(AccessStatus.Open)
    else
      varfield
        .subfieldsWithTag("f")
        .contents
        .headOption
        .map { str =>
          AccessStatus(str) match {
            case Left(err)     => throw err
            case Right(status) => status
          }
        }
}
