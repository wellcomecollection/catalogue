package weco.catalogue.internal_model

import io.circe.generic.extras.semiauto._
import io.circe._
import weco.catalogue.internal_model.identifiers._
import weco.catalogue.internal_model.image._
import weco.catalogue.internal_model.locations.{
  AccessCondition,
  DigitalLocation,
  Location,
  PhysicalLocation
}
import weco.catalogue.internal_model.matcher.MatcherResult
import weco.catalogue.internal_model.work._
import weco.json.JsonUtil._

object Implicits {

  // Cache these here to improve compilation times (otherwise they are
  // re-derived every time they are required).
  //
  // The particular implicits defined here have been chosen by generating
  // flamegraphs using the scalac-profiling plugin. See this blog post for
  // info: https://www.scala-lang.org/blog/2018/06/04/scalac-profiling.html
  //
  // NOTE: the ordering here is important: we derive ImageData[_]
  // then WorkData, then the other images and works due to the order of
  // dependencies (thus preventing duplicate work)

  implicit val _dec00: Decoder[AccessCondition] = deriveConfiguredDecoder
  implicit val _dec01: Decoder[Note] = deriveConfiguredDecoder
  implicit val _dec02: Decoder[SourceIdentifier] = deriveConfiguredDecoder
  implicit val _dec03: Decoder[IdState.Unminted] = deriveConfiguredDecoder
  implicit val _dec04: Decoder[IdState.Minted] = deriveConfiguredDecoder
  implicit val _dec05: Decoder[InstantRange] = deriveConfiguredDecoder
  implicit val _dec06: Decoder[DigitalLocation] =
    deriveConfiguredDecoder
  implicit val _dec07: Decoder[PhysicalLocation] =
    deriveConfiguredDecoder
  implicit val _dec08: Decoder[Location] = deriveConfiguredDecoder
  implicit val _dec11: Decoder[MergeCandidate[IdState.Identifiable]] =
    deriveConfiguredDecoder
  implicit val _dec12: Decoder[MergeCandidate[IdState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec13: Decoder[MatcherResult] = deriveConfiguredDecoder
  implicit val _dec14: Decoder[Person[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec15: Decoder[Person[IdState.Minted]] = deriveConfiguredDecoder
  implicit val _dec16: Decoder[Meeting[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec17: Decoder[Meeting[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec18: Decoder[Organisation[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec19: Decoder[Organisation[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec20: Decoder[Agent[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec21: Decoder[Agent[IdState.Minted]] = deriveConfiguredDecoder
  implicit val _dec22: Decoder[AbstractAgent[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec23: Decoder[AbstractAgent[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec24: Decoder[Place[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec25: Decoder[Period[IdState.Minted]] = deriveConfiguredDecoder
  implicit val _dec26: Decoder[Concept[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec27: Decoder[Concept[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec28: Decoder[AbstractConcept[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec29: Decoder[AbstractConcept[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec30: Decoder[AbstractRootConcept[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec31: Decoder[AbstractRootConcept[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec32: Decoder[Genre[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec33: Decoder[Genre[IdState.Minted]] = deriveConfiguredDecoder
  implicit val _dec34: Decoder[Subject[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec35: Decoder[Subject[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec36: Decoder[ProductionEvent[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec37: Decoder[ProductionEvent[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec38: Decoder[Item[IdState.Unminted]] = deriveConfiguredDecoder
  implicit val _dec39: Decoder[Item[IdState.Minted]] = deriveConfiguredDecoder
  implicit val _dec40: Decoder[Contributor[IdState.Unminted]] =
    deriveConfiguredDecoder
  implicit val _dec41: Decoder[Contributor[IdState.Minted]] =
    deriveConfiguredDecoder
  implicit val _dec42: Decoder[ImageData[IdState.Identifiable]] =
    deriveConfiguredDecoder
  implicit val _dec43: Decoder[ImageData[IdState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec44: Decoder[CollectionPath] =
    deriveConfiguredDecoder
  implicit val _dec45: Decoder[WorkData[DataState.Unidentified]] =
    deriveConfiguredDecoder
  implicit val _dec46: Decoder[WorkData[DataState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec47: Decoder[Work.Visible[WorkState.Source]] =
    deriveConfiguredDecoder
  implicit val _dec48: Decoder[Work.Visible[WorkState.Merged]] =
    deriveConfiguredDecoder
  implicit val _dec49: Decoder[Work.Visible[WorkState.Denormalised]] =
    deriveConfiguredDecoder
  implicit val _dec50: Decoder[Work.Visible[WorkState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec51: Decoder[Work.Invisible[WorkState.Source]] =
    deriveConfiguredDecoder
  implicit val _dec52: Decoder[Work.Invisible[WorkState.Merged]] =
    deriveConfiguredDecoder
  implicit val _dec53: Decoder[Work.Invisible[WorkState.Denormalised]] =
    deriveConfiguredDecoder
  implicit val _dec54: Decoder[Work.Invisible[WorkState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec55: Decoder[Work.Redirected[WorkState.Source]] =
    deriveConfiguredDecoder
  implicit val _dec56: Decoder[Work.Redirected[WorkState.Merged]] =
    deriveConfiguredDecoder
  implicit val _dec57: Decoder[Work.Redirected[WorkState.Denormalised]] =
    deriveConfiguredDecoder
  implicit val _dec58: Decoder[Work.Redirected[WorkState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec59: Decoder[Work[WorkState.Source]] =
    deriveConfiguredDecoder
  implicit val _dec60: Decoder[Work[WorkState.Merged]] =
    deriveConfiguredDecoder
  implicit val _dec61: Decoder[Work[WorkState.Denormalised]] =
    deriveConfiguredDecoder
  implicit val _dec62: Decoder[Work[WorkState.Identified]] =
    deriveConfiguredDecoder
  implicit val _dec63: Decoder[ParentWorks] =
    deriveConfiguredDecoder
  implicit val _dec64: Decoder[ImageSource] =
    deriveConfiguredDecoder
  implicit val _dec65: Decoder[Image[ImageState.Initial]] =
    deriveConfiguredDecoder
  implicit val _dec66: Decoder[Image[ImageState.Augmented]] =
    deriveConfiguredDecoder
  implicit val _dec67: Decoder[Image[ImageState.Indexed]] =
    deriveConfiguredDecoder
  implicit val _dec68: Decoder[Work[WorkState.Indexed]] =
    deriveConfiguredDecoder
  implicit val _dec69: Decoder[Work.Visible[WorkState.Indexed]] =
    deriveConfiguredDecoder
  implicit val _dec70: Decoder[Work.Invisible[WorkState.Indexed]] =
    deriveConfiguredDecoder
  implicit val _dec71: Decoder[Work.Redirected[WorkState.Indexed]] =
    deriveConfiguredDecoder

  implicit val _enc00: Encoder[AccessCondition] = deriveConfiguredEncoder
  implicit val _enc01: Encoder[Note] = deriveConfiguredEncoder
  implicit val _enc02: Encoder[SourceIdentifier] = deriveConfiguredEncoder
  implicit val _enc03: Encoder[IdState.Unminted] = deriveConfiguredEncoder
  implicit val _enc04: Encoder[IdState.Minted] = deriveConfiguredEncoder
  implicit val _enc05: Encoder[InstantRange] = deriveConfiguredEncoder
  implicit val _enc06: Encoder[DigitalLocation] =
    deriveConfiguredEncoder
  implicit val _enc07: Encoder[PhysicalLocation] =
    deriveConfiguredEncoder
  implicit val _enc08: Encoder[Location] = deriveConfiguredEncoder
  implicit val _enc11
    : Encoder[MergeCandidate[ImageData[IdState.Identifiable]]] =
    deriveConfiguredEncoder
  implicit val _enc12: Encoder[MergeCandidate[ImageData[IdState.Identified]]] =
    deriveConfiguredEncoder
  implicit val _enc13: Encoder[MatcherResult] = deriveConfiguredEncoder
  implicit val _enc14: Encoder[Person[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc15: Encoder[Person[IdState.Minted]] = deriveConfiguredEncoder
  implicit val _enc16: Encoder[Meeting[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc17: Encoder[Meeting[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc18: Encoder[Organisation[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc19: Encoder[Organisation[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc20: Encoder[Agent[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc21: Encoder[Agent[IdState.Minted]] = deriveConfiguredEncoder
  implicit val _enc22: Encoder[AbstractAgent[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc23: Encoder[AbstractAgent[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc24: Encoder[Place[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc25: Encoder[Period[IdState.Minted]] = deriveConfiguredEncoder
  implicit val _enc26: Encoder[Concept[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc27: Encoder[Concept[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc28: Encoder[AbstractConcept[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc29: Encoder[AbstractConcept[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc30: Encoder[AbstractRootConcept[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc31: Encoder[AbstractRootConcept[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc32: Encoder[Genre[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc33: Encoder[Genre[IdState.Minted]] = deriveConfiguredEncoder
  implicit val _enc34: Encoder[Subject[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc35: Encoder[Subject[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc36: Encoder[ProductionEvent[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc37: Encoder[ProductionEvent[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc38: Encoder[Item[IdState.Unminted]] = deriveConfiguredEncoder
  implicit val _enc39: Encoder[Item[IdState.Minted]] = deriveConfiguredEncoder
  implicit val _enc40: Encoder[Contributor[IdState.Unminted]] =
    deriveConfiguredEncoder
  implicit val _enc41: Encoder[Contributor[IdState.Minted]] =
    deriveConfiguredEncoder
  implicit val _enc42: Encoder[ImageData[IdState.Identifiable]] =
    deriveConfiguredEncoder
  implicit val _enc43: Encoder[ImageData[IdState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc44: Encoder[CollectionPath] =
    deriveConfiguredEncoder
  implicit val _enc45: Encoder[WorkData[DataState.Unidentified]] =
    deriveConfiguredEncoder
  implicit val _enc46: Encoder[WorkData[DataState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc47: Encoder[Work.Visible[WorkState.Source]] =
    deriveConfiguredEncoder
  implicit val _enc48: Encoder[Work.Visible[WorkState.Merged]] =
    deriveConfiguredEncoder
  implicit val _enc49: Encoder[Work.Visible[WorkState.Denormalised]] =
    deriveConfiguredEncoder
  implicit val _enc50: Encoder[Work.Visible[WorkState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc51: Encoder[Work.Invisible[WorkState.Source]] =
    deriveConfiguredEncoder
  implicit val _enc52: Encoder[Work.Invisible[WorkState.Merged]] =
    deriveConfiguredEncoder
  implicit val _enc53: Encoder[Work.Invisible[WorkState.Denormalised]] =
    deriveConfiguredEncoder
  implicit val _enc54: Encoder[Work.Invisible[WorkState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc55: Encoder[Work.Redirected[WorkState.Source]] =
    deriveConfiguredEncoder
  implicit val _enc56: Encoder[Work.Redirected[WorkState.Merged]] =
    deriveConfiguredEncoder
  implicit val _enc57: Encoder[Work.Redirected[WorkState.Denormalised]] =
    deriveConfiguredEncoder
  implicit val _enc58: Encoder[Work.Redirected[WorkState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc59: Encoder[Work[WorkState.Source]] =
    deriveConfiguredEncoder
  implicit val _enc60: Encoder[Work[WorkState.Merged]] =
    deriveConfiguredEncoder
  implicit val _enc61: Encoder[Work[WorkState.Denormalised]] =
    deriveConfiguredEncoder
  implicit val _enc62: Encoder[Work[WorkState.Identified]] =
    deriveConfiguredEncoder
  implicit val _enc63: Encoder[ParentWorks] =
    deriveConfiguredEncoder
  implicit val _enc64: Encoder[ImageSource] =
    deriveConfiguredEncoder
  implicit val _enc65: Encoder[Image[ImageState.Initial]] =
    deriveConfiguredEncoder
  implicit val _enc66: Encoder[Image[ImageState.Augmented]] =
    deriveConfiguredEncoder
  implicit val _enc67: Encoder[Image[ImageState.Indexed]] =
    deriveConfiguredEncoder
  implicit val _enc68: Encoder[Work[WorkState.Indexed]] =
    deriveConfiguredEncoder
  implicit val _enc69: Encoder[Work.Visible[WorkState.Indexed]] =
    deriveConfiguredEncoder
  implicit val _enc70: Encoder[Work.Invisible[WorkState.Indexed]] =
    deriveConfiguredEncoder
  implicit val _enc71: Encoder[Work.Redirected[WorkState.Indexed]] =
    deriveConfiguredEncoder
}
