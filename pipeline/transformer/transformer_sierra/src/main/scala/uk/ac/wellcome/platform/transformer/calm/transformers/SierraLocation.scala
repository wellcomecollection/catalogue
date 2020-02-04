package uk.ac.wellcome.platform.transformer.calm.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.calm.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.calm.source.{
  SierraBibData,
  SierraItemData,
  SierraQueryOps,
  VarField
}
import uk.ac.wellcome.platform.transformer.calm.source.sierra.SierraSourceLocation

trait SierraLocation extends SierraQueryOps {

  def getPhysicalLocation(itemData: SierraItemData,
                          bibData: SierraBibData): Option[PhysicalLocation] =
    itemData.location.flatMap {
      // We've seen records where the "location" field is populated in
      // the JSON, but the code and name are both empty strings or "none".
      // We can't do anything useful with this, so don't return a location.
      case SierraSourceLocation("", "")         => None
      case SierraSourceLocation("none", "none") => None
      case SierraSourceLocation(code, name) =>
        Some(
          PhysicalLocation(
            locationType = LocationType(code),
            accessConditions = getAccessConditions(bibData),
            label = name
          )
        )
    }

  def getDigitalLocation(identifier: String): DigitalLocation = {
    // This is a defensive check, it may not be needed since an identifier should always be present.
    if (!identifier.isEmpty) {
      DigitalLocation(
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
          terms = varfield.subfieldsWithTag("a").contentString,
          to = varfield.subfieldsWithTag("g").contents.headOption
        )
      }
      .filter {
        case AccessCondition(None, None, None) => false
        case _                                 => true
      }

  private def getAccessStatus(varfield: VarField): Option[AccessStatus] = {
    val accessStatus = """([A-Za-z\s\(\)]+)\p{Punct}?""".r
    if (varfield.indicator1 == Some("0"))
      Some(AccessStatus.Open)
    else
      varfield
        .subfieldsWithTag("f")
        .contents
        .headOption
        .map {
          case accessStatus(status) if status == "Open" => AccessStatus.Open
          case accessStatus(status) if status == "Open with advisory" =>
            AccessStatus.OpenWithAdvisory
          case accessStatus(status) if status == "Restricted" =>
            AccessStatus.Restricted
          case accessStatus(status)
              if status == "Restricted access (Data Protection Act)" =>
            AccessStatus.Restricted
          case accessStatus(status) if status == "Cannot Be Produced" =>
            AccessStatus.Restricted
          case accessStatus(status) if status == "Certain restrictions apply" =>
            AccessStatus.Restricted
          case accessStatus(status) if status == "Closed" => AccessStatus.Closed
          case status =>
            throw new Exception(s"Unrecognised AccessStatus: $status")
        }
  }
}
