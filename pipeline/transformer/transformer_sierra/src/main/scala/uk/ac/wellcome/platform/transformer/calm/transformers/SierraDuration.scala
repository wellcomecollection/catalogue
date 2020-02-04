package uk.ac.wellcome.platform.transformer.calm.transformers

import scala.util.Try
import scala.concurrent.duration._

import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.platform.transformer.calm.source.{
  SierraBibData,
  SierraQueryOps
}

object SierraDuration extends SierraTransformer with SierraQueryOps {

  type Output = Option[Int]

  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    bibData
      .subfieldsWithTag("306" -> "a")
      .firstContent
      .map { durationString =>
        durationString
          .grouped(2)
          .map(substr => Try(substr.toInt).toOption)
          .toSeq
      }
      .collect {
        case Seq(Some(hours), Some(minutes), Some(seconds)) =>
          (hours.hours + minutes.minutes + seconds.seconds).toMillis.toInt
      }
}
