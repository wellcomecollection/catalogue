package uk.ac.wellcome.sierra_adapter.model

import java.time.Instant

import uk.ac.wellcome.fixtures.RandomGenerators
import uk.ac.wellcome.json.JsonUtil._

import scala.util.Random

trait SierraGenerators extends RandomGenerators {
  // A lot of Sierra tests (e.g. mergers) check the behaviour when merging
  // a record with a newer version, or vice versa.  Provide two dates here
  // for convenience.
  val olderDate: Instant = Instant.parse("1999-09-09T09:09:09Z")
  val newerDate: Instant = Instant.parse("2001-01-01T01:01:01Z")

  /** Returns a random digit as a string.  This is copied from the
    * definition of Random.alphanumeric.
    */
  private def randomNumeric: Stream[Char] = {
    def nextDigit: Char = {
      val chars = '0' to '9'
      chars charAt Random.nextInt(chars.length)
    }

    Stream continually nextDigit
  }

  def createSierraRecordNumberString: String =
    randomNumeric take 7 mkString

  def createSierraBibNumber: SierraBibNumber =
    SierraBibNumber(createSierraRecordNumberString)

  def createSierraBibNumbers(count: Int): List[SierraBibNumber] =
    (1 to count).map { _ =>
      createSierraBibNumber
    }.toList

  def createSierraItemNumber: SierraItemNumber =
    SierraItemNumber(createSierraRecordNumberString)

  def createSierraHoldingsNumber: SierraHoldingsNumber =
    SierraHoldingsNumber(createSierraRecordNumberString)

  protected def createTitleVarfield(
    title: String = s"title-${randomAlphanumeric()}"): String =
    s"""
       |{
       |  "marcTag": "245",
       |  "subfields": [
       |    {"tag": "a", "content": "$title"}
       |  ]
       |}
     """.stripMargin

  def createSierraBibRecordWith(
    id: SierraBibNumber = createSierraBibNumber,
    data: String = "",
    modifiedDate: Instant = olderDate): SierraBibRecord = {

    val recordData = if (data == "") {
      s"""
         |{
         |  "id": "$id",
         |  "varFields": [
         |    ${createTitleVarfield()}
         |  ]
         |}""".stripMargin
    } else data

    SierraBibRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate
    )
  }

  def createSierraBibRecord: SierraBibRecord = createSierraBibRecordWith()

  def createSierraItemRecordWith(
    id: SierraItemNumber = createSierraItemNumber,
    data: (SierraItemNumber, Instant, List[SierraBibNumber]) => String =
      defaultData,
    modifiedDate: Instant = Instant.now,
    bibIds: List[SierraBibNumber] = List(),
    unlinkedBibIds: List[SierraBibNumber] = List()
  ): SierraItemRecord = {
    val recordData = data(id, modifiedDate, bibIds)

    SierraItemRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )
  }

  def createSierraHoldingsRecordWith(
    id: SierraHoldingsNumber = createSierraHoldingsNumber,
    data: (SierraHoldingsNumber, Instant, List[SierraBibNumber]) => String =
      defaultData,
    modifiedDate: Instant = Instant.now,
    bibIds: List[SierraBibNumber] = List(),
    unlinkedBibIds: List[SierraBibNumber] = List()
  ): SierraHoldingsRecord = {
    val recordData = data(id, modifiedDate, bibIds)

    SierraHoldingsRecord(
      id = id,
      data = recordData,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )
  }

  private def defaultData(id: SierraTypedRecordNumber,
                          modifiedDate: Instant,
                          bibIds: List[SierraBibNumber]): String =
    s"""
       |{
       |  "id": "$id",
       |  "updatedDate": "${modifiedDate.toString}",
       |  "bibIds": ${toJson(bibIds.map(_.recordNumber)).get}
       |}
       |""".stripMargin

  def createSierraItemRecord: SierraItemRecord = createSierraItemRecordWith()

  def createSierraTransformableWith(
    sierraId: SierraBibNumber = createSierraBibNumber,
    maybeBibRecord: Option[SierraBibRecord] = Some(createSierraBibRecord),
    itemRecords: List[SierraItemRecord] = List()
  ): SierraTransformable =
    SierraTransformable(
      sierraId = sierraId,
      maybeBibRecord = maybeBibRecord,
      itemRecords = itemRecords.map { record: SierraItemRecord =>
        record.id -> record
      }.toMap
    )

  def createSierraTransformable: SierraTransformable =
    createSierraTransformableWith()
}
