package uk.ac.wellcome.platform.calm_api_client

import java.time.Instant

case class CalmRecord(
  id: String,
  data: Map[String, List[String]],
  retrievedAt: Instant,
  published: Boolean = false
) {

  def refNo: Option[String] =
    data.get("RefNo").flatMap(_.headOption)

  def modified: Option[String] =
    data.get("Modified").flatMap(_.headOption)
}
