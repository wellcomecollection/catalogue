package weco.pipeline.transformer.sierra.transformers

import grizzled.slf4j.Logging

import java.util.UUID
import weco.catalogue.internal_model.identifiers.{
  IdState,
  IdentifierType,
  SourceIdentifier
}
import weco.catalogue.internal_model.work.MergeCandidate
import weco.catalogue.source_model.sierra.source.SierraQueryOps
import weco.catalogue.source_model.sierra.SierraBibData
import weco.catalogue.source_model.sierra.identifiers.SierraBibNumber
import weco.pipeline.transformer.sierra.transformers.parsers.MiroIdParsing

import scala.util.Try
import scala.util.matching.Regex

object SierraMergeCandidates
    extends SierraIdentifiedDataTransformer
    with SierraQueryOps
    with Logging {

  type Output = List[MergeCandidate[IdState.Identifiable]]

  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    get776mergeCandidates(bibId, bibData) ++
      getSinglePageMiroMergeCandidates(bibData) ++ get035CalmMergeCandidates(
      bibData)

  // This regex matches any string starting with (UkLW), followed by
  // any number of spaces, and then captures everything after the
  // space, which is the bib number we're interested in.
  //
  // The UkLW match is case insensitive because there are sufficient
  // inconsistencies in the source data that it's easier to handle that here.
  private val uklwPrefixRegex: Regex = """\((?i:UkLW)\)[\s]*(.+)""".r.anchored

  private val bnumberRegex: Regex = """b[0-9]{7}[0-9x]""".r.anchored

  /** We can merge a bib and the digitised version of that bib.  The number
    * of the other bib comes from MARC tag 776 subfield $w.
    *
    * If the identifier starts with (UkLW), we strip the prefix and use the
    * bib number as a merge candidate.
    *
    * We ignore any values in 776 subfield ǂw that don't start with (UkLW),
    * e.g. identifiers that start (OCLC).
    *
    */
  private def get776mergeCandidates(
    bibId: SierraBibNumber,
    bibData: SierraBibData): List[MergeCandidate[IdState.Identifiable]] = {

    val identifiers =
      bibData
        .subfieldsWithTag("776" -> "w")
        .contents
        .flatMap {
          case uklwPrefixRegex(bibNumber) => Some(bibNumber.trim)
          case _                          => None
        }
        .filter { s =>
          bnumberRegex.findFirstIn(s) match {
            case Some(_) => true
            case None =>
              warn(s"$bibId: not a b number in field 776 subfield ǂw: $s")
              false
          }
        }

    identifiers.distinct match {
      case List(bibNumber) =>
        List(
          MergeCandidate(
            identifier = SourceIdentifier(
              identifierType = IdentifierType.SierraSystemNumber,
              ontologyType = "Work",
              value = bibNumber
            ),
            reason = "Physical/digitised Sierra work"
          )
        )
      case _ => Nil
    }
  }

  /*
   * We always try to merge all linked Miro and Sierra works
   */
  private def getSinglePageMiroMergeCandidates(
    bibData: SierraBibData): List[MergeCandidate[IdState.Identifiable]] = {
    val allIds = (matching089Ids(bibData) ++ matching962Ids(bibData)).distinct
    removeNonSuffixedIfSuffixedExists(allIds).map {
      miroMergeCandidate(_, "Miro/Sierra work")
    }
  }

  /** When we harvest the Calm data into Sierra, the `RecordID` is stored in
    * Marcfield 035$a.
    *
    * Calm IDs follow the UUID spec
    *
    * e.g: f5217b45-b742-472b-95c3-f136d5de1104
    * see: `https://search.wellcomelibrary.org/iii/encore/record/C__Rb1971204?marcData=Y`
    *
    * This field is also used for other "system control numbers" from UKMHL, LSHTM etc.
    * e.g: (OCoLC)927468903, (lshtm)a60032
    * see: `https://search.wellcomelibrary.org/iii/encore/record/C__Rb1187988?marcData=Y`
    */
  private def get035CalmMergeCandidates(
    bibData: SierraBibData): List[MergeCandidate[IdState.Identifiable]] =
    bibData
      .subfieldsWithTag("035" -> "a")
      .contents
      .flatMap(str => Try(UUID.fromString(str)).toOption)
      .map { recordId =>
        MergeCandidate(
          identifier = SourceIdentifier(
            identifierType = IdentifierType.CalmRecordIdentifier,
            ontologyType = "Work",
            value = recordId.toString
          ),
          reason = "Calm/Sierra harvest"
        )
      }
      .distinct

  private def matching962Ids(bibData: SierraBibData) =
    bibData
      .subfieldsWithTag("962" -> "u")
      .contents
      .flatMap { MiroIdParsing.maybeFromURL }
      .distinct

  private def matching089Ids(bibData: SierraBibData) =
    bibData
      .subfieldsWithTag("089" -> "a")
      .contents
      .flatMap { MiroIdParsing.maybeFromString }

  private def miroMergeCandidate(miroId: String, reason: String) = {
    MergeCandidate(
      identifier = SourceIdentifier(
        identifierType = IdentifierType.MiroImageNumber,
        ontologyType = "Work",
        value = miroId
      ),
      reason = reason
    )
  }

  // If we have IDs that are identical except for a non-numeric suffix,
  // then we want to use the suffixed IDs over the the non-suffixed ones.
  //
  // eg. if we have V0036036EL and V0036036, we want to remove
  // V0036036 and keep V0036036EL.
  private def removeNonSuffixedIfSuffixedExists(
    ids: List[String]): List[String] =
    ids
      .groupBy(MiroIdParsing.stripSuffix)
      .flatMap {
        case (_, List(singleId)) => List(singleId)
        case (stemId, leafIds)   => leafIds.filterNot(_ == stemId)
      }
      .toList
}
