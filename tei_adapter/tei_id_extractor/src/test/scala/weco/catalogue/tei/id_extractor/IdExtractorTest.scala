package weco.catalogue.tei.id_extractor

import org.apache.commons.io.IOUtils
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import scala.util.{Failure, Success}

class IdExtractorTest extends AnyFunSpec with Matchers {
  val path = "Arabic/WMS_arabic.xml"
  it("extracts the id from a valid tei xml") {
    val triedId = IdExtractor.extractId(
      IOUtils.resourceToString("/WMS_Arabic_1.xml", StandardCharsets.UTF_8),
      path)
    triedId shouldBe a[Success[_]]
    triedId.get shouldBe "manuscript_15651"
  }
  it("extracts the id from a valid tei xml with BOM") {
    val triedId = IdExtractor.extractId(
      IOUtils.resourceToString("/Javanese_11.xml", StandardCharsets.UTF_8),
      path)
    triedId shouldBe a[Success[_]]
    triedId.get shouldBe "Well.Jav.11"
  }

  it("fails if xml is invalid") {
    val triedId = IdExtractor.extractId("not an xml", path)
    triedId shouldBe a[Failure[_]]
  }
  it("fails if xml is valid but it's not tei") {
    val triedId =
      IdExtractor.extractId("<root><text>not a tei</text/></root>", path)
    triedId shouldBe a[Failure[_]]
  }
  it("fails if tei xml does not have a xml:id property") {

    val triedId = IdExtractor.extractId(
      """<?xml version="1.0" encoding="UTF-8"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://relaxng.org/ns/structure/1.0"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://purl.oclc.org/dsdl/schematron"?>
    <TEI xmlns="http://www.tei-c.org/ns/1.0"></TEI>""",
      path
    )
    triedId shouldBe a[Failure[_]]
    triedId.failed.get shouldBe a[RuntimeException]
    triedId.failed.get.getMessage should include(path.toString)
  }
  it("fails if tei xml has more than one xml:id property") {

    val triedId = IdExtractor.extractId(
      """<?xml version="1.0" encoding="UTF-8"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://relaxng.org/ns/structure/1.0"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://purl.oclc.org/dsdl/schematron"?>
    <TEI xmlns="http://www.tei-c.org/ns/1.0" xml:id="manuscript_12345" xml:id="manuscript_5678"></TEI>""",
      path
    )
    triedId shouldBe a[Failure[_]]
    triedId.failed.get shouldBe a[RuntimeException]
    triedId.failed.get.getMessage should include(path.toString)
  }
  it("does not read the xml:id from anywhere else in the xml") {

    val triedId = IdExtractor.extractId(
      """<?xml version="1.0" encoding="UTF-8"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://relaxng.org/ns/structure/1.0"?>
    <?xml-model href="https://raw.githubusercontent.com/bodleian/consolidated-tei-schema/master/msdesc.rng" type="application/xml" schematypens="http://purl.oclc.org/dsdl/schematron"?>
    <TEI xmlns="http://www.tei-c.org/ns/1.0"><child xml:id="manuscript_12345"></child></TEI>""",
      path
    )
    triedId shouldBe a[Failure[_]]
    triedId.failed.get shouldBe a[RuntimeException]
    triedId.failed.get.getMessage should include(path.toString)
  }
}
