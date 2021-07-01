package uk.ac.wellcome.platform.transformer.tei.transformer

import weco.catalogue.internal_model.identifiers.DataState.Unidentified
import weco.catalogue.internal_model.identifiers.{IdentifierType, SourceIdentifier}
import weco.catalogue.internal_model.work.WorkState.Source
import weco.catalogue.internal_model.work.{MergeCandidate, Work, WorkData}

import java.time.Instant

object TeiDataParser {
  def parse(teiXml: TeiXml): Either[Throwable, TeiData] =
    for {
      summary <- teiXml.summary
      bNumber <- teiXml.bNumber
    } yield TeiData(teiXml.id, summary, bNumber)
}

case class TeiData(
  id: String,
  description: Option[String] = None,
  bNumber: Option[String]
) {
  def toWork(time: Instant, version: Int): Work[Source] = {
    val maybeBnumber = bNumber
      .map(
        b =>
          MergeCandidate(
            SourceIdentifier(IdentifierType.SierraSystemNumber, "Work", b),
            "Bnumber present in TEI file"
          )
      )

    val value =
      WorkData[Unidentified](description = description, mergeCandidates = maybeBnumber.toList)
    Work.Visible[Source](
      version,
      value,
      state = Source(SourceIdentifier(IdentifierType.Tei, "Work", id), time),
      Nil
    )
  }
}




