package uk.ac.wellcome.platform.calm_api_client

import scala.xml.Elem

trait CalmXmlRequest {
  type Response

  val action: String

  def body: Elem

  def xml: Elem =
    <soap12:Envelope
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
        xmlns:soap12="http://www.w3.org/2003/05/soap-envelope">
      <soap12:Body>
        {body}
      </soap12:Body>
    </soap12:Envelope>
}

case class CalmSearchRequest(query: CalmQuery, dbName: String = "Catalog")
    extends CalmXmlRequest {
  type Response = CalmSession
  val action = "Search"

  def body: Elem =
    <Search xmlns="http://ds.co.uk/cs/webservices/">
      <dbname>{dbName}</dbname>
      <elementSet>DC</elementSet>
      <expr>{query.queryExpression}</expr>
    </Search>
}

case class CalmSummaryRequest(pos: Int, dbName: String = "Catalog")
    extends CalmXmlRequest {
  type Response = CalmRecord
  val action = "SummaryHeader"

  def body: Elem =
    <SummaryHeader xmlns="http://ds.co.uk/cs/webservices/">
      <dbname>{dbName}</dbname>
      <HitLstPos>{pos}</HitLstPos>
    </SummaryHeader>
}
