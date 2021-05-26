package weco.catalogue.source_model.sierra.rules

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.json.JsonUtil._
import weco.catalogue.internal_model.locations.PhysicalLocationType
import weco.catalogue.source_model.sierra.Implicits._
import weco.catalogue.source_model.sierra.{SierraBibData, SierraBibNumber, SierraItemData, SierraItemNumber, SierraTransformable}

import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import scala.util.{Failure, Success, Try}

class SierraItemAccessTest extends AnyFunSpec with Matchers {
  it("assigns access conditions for all Sierra items") {
    // Note: this test is not meant to hang around long-term.  It's a test harness
    // that runs through every SierraTransformable instance, tries to assign some
    // access conditions, and counts how many it can't handle.
    //
    // Looking at the items that can't be assigned access conditions helps us
    // find what needs fixing in the data/transformer.
    val reader: Iterator[String] =
      new Iterator[String] {
        val reader =
          new BufferedReader(
            new InputStreamReader(
              new FileInputStream("/Users/alexwlchan/desktop/sierra/out.json")))

        override def hasNext: Boolean = reader.ready
        override def next(): String = reader.readLine()
      }

    val bibItemPairs: Iterator[(SierraBibNumber, SierraBibData, SierraItemNumber, SierraItemData)] =
      reader
        .flatMap { json =>
          val t = fromJson[SierraTransformable](json).get

          t.maybeBibRecord match {
            case Some(bibRecord) =>
              val bibData = fromJson[SierraBibData](bibRecord.data).get
              t.itemRecords.values.toList.map { itemRecord =>
                val itemData = fromJson[SierraItemData](itemRecord.data).get
                (bibRecord.id, bibData, itemRecord.id, itemData)
              }

            case None => List()
          }
        }

    var handled = 0
    var unhandled = 0

    bibItemPairs
      .filterNot {
        case (_, bibData, _, itemData) =>
          bibData.suppressed | bibData.deleted | itemData.suppressed | itemData.deleted
      }
      .foreach {
        case (bibId, bibData, itemId, itemData) =>
          // Note: When we wire up these into the items/locations code, we'll pass
          // in these values rather than re-parse them, but this works well enough
          // for the test harness.
          val bibAccessStatus = SierraAccessStatus.forBib(bibId, bibData)
          val location: Option[PhysicalLocationType] = itemData.location
            .map { _.name }
            .flatMap { SierraPhysicalLocationType.fromName(itemId, _) }

          val ac = Try {
            SierraItemAccess(bibAccessStatus, location, itemData)
          }

          // Print the bib/item data for the first 100 failures
          if (unhandled < 100) {
            println(bibId.withCheckDigit)
            println(bibData.varFields.filter(_.marcTag.contains("506")))
            println(itemId.withCheckDigit)
            println(itemData.location)
            println(itemData.fixedFields.filterNot {
              case (code, _) =>
                Set(
                  "68",
                  "63",
                  "71",
                  "72",
                  "80",
                  "67",
                  "66",
                  "69",
                  "78",
                  "109",
                  "162",
                  "264",
                  "161",
                  "306",
                  "70",
                  "86",
                  "64",
                  "81",
                  "59",
                  "64",
                  "76",
                  "98",
                  "93",
                  "84",
                  "265",
                  "62",
                  "83",
                  "77",
                  "110",
                  "60",
                  "94",
                  "127",
                  "57",
                  "58",
                  "74",
                  "85"
                ).contains(code)
            })
            println(itemData.varFields.filter(_.fieldTag.contains("n")))
            println("")
          }

          ac match {
            case Success(_) => handled += 1
            case Failure(_) => unhandled += 1
          }
      }

    println(s"$handled handled, $unhandled unhandled")
  }
}