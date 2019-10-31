package uk.ac.wellcome.models.work.internal

sealed trait Note {
  val content: String
}

case class GeneralNote(val content: String) extends Note

case class BibliographicalInformation(val content: String) extends Note

case class FundingInformation(val content: String) extends Note

case class TimeAndPlaceNote(val content: String) extends Note

case class CreditsNote(val content: String) extends Note

case class ContentsNote(val content: String) extends Note

case class DissertationNote(val content: String) extends Note

case class CiteAsNote(val content: String) extends Note

case class LocationOfOriginalNote(val content: String) extends Note

case class BindingInformation(val content: String) extends Note
