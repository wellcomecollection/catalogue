package uk.ac.wellcome.platform.id_minter.steps

import grizzled.slf4j.Logging
import io.circe.Json
import io.circe.optics.JsonPath.root
import io.circe.optics.JsonOptics._
import monocle.function.Plated
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.platform.id_minter.models.Identifier
import weco.catalogue.internal_model.identifiers.{CanonicalID, SourceIdentifier}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * SourceIdentifierEmbedder provides 2 methods:
  *
  * - `scan` takes Json and returns all of the sourceIdentifiers that are in it
  * - `update` takes Json and a map of (SourceIdentifier -> Identifier) and adds
  *   a canonicalId field next to sourceIdentifiers, as well as replacing
  *   `identifiedType` fields with `type` fields of the same value.
  *
  */
object SourceIdentifierEmbedder extends Logging {
  def scan(inputJson: Json): Try[List[SourceIdentifier]] =
    Try(
      iterate(
        root.each.json.getAll(inputJson),
        root.sourceIdentifier.json
          .getOption(inputJson)
          .map(parseSourceIdentifier)
          .toList
      )
    )

  def update(inputJson: Json,
             identifiers: Map[SourceIdentifier, Identifier]): Try[Json] =
    Try {
      val updateNode =
        (updateNodeType _) compose addCanonicalIdToNode(identifiers)
      // Plated transforms do not operate on the top-level node of
      // as structure, so we need to update the root separately
      val updatedRoot = updateNode(inputJson)
      Plated.transform[Json](updateNode)(updatedRoot)
    }

  private def updateNodeType(node: Json): Json =
    root.identifiedType.json
      .getOption(node)
      .flatMap(_.asString)
      .map { identifiedType =>
        root.obj.modify { obj =>
          ("type", Json.fromString(identifiedType)) +:
            obj.remove("identifiedType")
        }(node)
      }
      .getOrElse(node)

  private def addCanonicalIdToNode(
    identifiers: Map[SourceIdentifier, Identifier])(node: Json): Json =
    root.sourceIdentifier.json
      .getOption(node)
      .map(parseSourceIdentifier)
      .map(getCanonicalId(identifiers))
      .map { canonicalId =>
        root.obj.modify { obj =>
          ("canonicalId", Json.fromString(canonicalId.underlying)) +: obj
        }(node)
      }
      .getOrElse(node)

  private def getCanonicalId(identifiers: Map[SourceIdentifier, Identifier])(
    sourceIdentifier: SourceIdentifier): CanonicalID =
    identifiers
      .getOrElse(
        sourceIdentifier,
        throw new RuntimeException(
          s"Could not find $sourceIdentifier in $identifiers")
      )
      .CanonicalId

  @tailrec
  private def iterate(
    children: List[Json],
    identifiers: List[SourceIdentifier]): List[SourceIdentifier] =
    children match {
      case Nil => identifiers
      case headChild :: tailChildren =>
        iterate(
          root.each.json.getAll(headChild) ++ tailChildren,
          identifiers ++ root.sourceIdentifier.json
            .getOption(headChild)
            .map(parseSourceIdentifier)
        )
    }

  private def parseSourceIdentifier(
    sourceIdentifierJson: Json): SourceIdentifier = {
    sourceIdentifierJson.as[SourceIdentifier].toTry match {
      case Success(sourceIdentifier) => sourceIdentifier
      case Failure(exception) =>
        error(
          s"Error parsing source identifier: ${sourceIdentifierJson.spaces2}")
        throw exception
    }

  }

}
