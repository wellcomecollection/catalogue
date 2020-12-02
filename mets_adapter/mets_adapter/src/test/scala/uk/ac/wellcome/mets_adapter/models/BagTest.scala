package uk.ac.wellcome.mets_adapter.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import java.time.Instant

class BagTest extends AnyFunSpec with Matchers {

  describe("METS path") {
    it("parses METS file from Bag") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        files = List(
          "data/b30246039.xml" -> "v1/data/b30246039.xml",
          "data/alto/b30246039_0001.xml" -> "v1/data/alto/b30246039_0001.xml",
          "data/alto/b30246039_0002.xml" -> "v1/data/alto/b30246039_0002.xml",
        )
      )
      bag.metsFile shouldBe Right("v1/data/b30246039.xml")
    }

    it("parses METS file from Bag when not first file") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        files = List(
          "data/alto/b30246039_0001.xml" -> "v1/data/alto/b30246039_0001.xml",
          "data/b30246039.xml" -> "v1/data/b30246039.xml",
          "data/alto/b30246039_0002.xml" -> "v1/data/alto/b30246039_0002.xml",
        )
      )
      bag.metsFile shouldBe Right("v1/data/b30246039.xml")
    }

    it("parses METS file from Bag when b-number ending with x") {
      val bag = createBag(
        s3Path = "digitised/b3024603x",
        files = List("data/b3024603x.xml" -> "v1/data/b3024603x.xml")
      )
      bag.metsFile shouldBe Right("v1/data/b3024603x.xml")
    }

    it("doesn't parse METS file from Bag when name not prefixed with 'data/'") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        files = List("b30246039.xml" -> "v1/data/b30246039.xml")
      )
      bag.metsFile shouldBe a[Left[_, _]]
    }

    it("doesn't parse METS file from Bag when name isn't XML'") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        files = List("data/b30246039.txt" -> "v1/data/b30246039.xml")
      )
      bag.metsFile shouldBe a[Left[_, _]]
    }
  }

  describe("bag version") {
    it("parses versions from Bag") {
      createBag(version = "v1").parsedVersion shouldBe Right(1)
      createBag(version = "v5").parsedVersion shouldBe Right(5)
      createBag(version = "v060").parsedVersion shouldBe Right(60)
    }

    it("doesn't parse incorrectly formatted versions") {
      createBag(version = "x1").parsedVersion shouldBe a[Left[_, _]]
      createBag(version = "v-1").parsedVersion shouldBe a[Left[_, _]]
      createBag(version = "1").parsedVersion shouldBe a[Left[_, _]]
    }
  }

  describe("METS manifestations") {
    it("parses a list of manifestations") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        files = List(
          "data/b30246039.txt" -> "v1/data/b30246039.xml",
          "data/alto/b30246039_0001.xml" -> "v1/data/alto/b30246039_0001.xml",
          "data/alto/b30246039_0002.xml" -> "v1/data/alto/b30246039_0002.xml",
          "data/b30246039_0001.xml" -> "v1/data/b30246039_0001.xml",
          "data/b30246039_0002.xml" -> "v1/data/b30246039_0002.xml",
        )
      )
      bag.manifestations shouldBe List(
        "v1/data/b30246039_0001.xml",
        "v1/data/b30246039_0002.xml")
    }
  }

  describe("METS data") {
    it("extracts all METS data from Bag") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        version = "v2",
        files = List("data/b30246039.xml" -> "v1/data/b30246039.xml",
          "objects/blahbluh.jp2" -> "v1/objects/blahbluh.jp2"),
      )
      bag.metsSourceData shouldBe Right(
        MetsSourceData(
          bucket = "bucket",
          path = "digitised/b30246039",
          version = 2,
          file = "v1/data/b30246039.xml",
          createdDate = bag.createdDate,
          deleted = false))
    }

    it("marks a METS data as deleted if there are no other assets except METS") {
      val bag = createBag(
        s3Path = "digitised/b30246039",
        version = "v2",
        files = List("data/b30246039.xml" -> "v1/data/b30246039.xml")
      )
      bag.metsSourceData shouldBe Right(
        MetsSourceData(
          bucket = "bucket",
          path = "digitised/b30246039",
          version = 2,
          file = "v1/data/b30246039.xml",
          createdDate = bag.createdDate,
          deleted = true))
    }

    it("fails extracting METS data if invalid version string") {
      val bag = createBag(
        version = "oops",
        files = List("data/b30246039.xml" -> "v1/data/b30246039.xml"),
      )
      bag.metsSourceData shouldBe a[Left[_, _]]
      bag.metsSourceData.left.get.getMessage shouldBe "Couldn't parse version"
    }

    it("fails extracting METS data if invalid no METS file") {
      val bag = createBag(files = Nil)
      bag.metsSourceData shouldBe a[Left[_, _]]
      bag.metsSourceData.left.get.getMessage shouldBe "Couldn't find METS file"
    }
  }

  def createBag(s3Path: String = "digitised/b30246039",
                version: String = "v1",
                files: List[(String, String)] = List(
                  "data/b30246039.xml" -> "v1/data/b30246039.xml")) =
    Bag(
      BagInfo("external-identifier"),
      BagManifest(
        files.map { case (name, path) => BagFile(name, path) }
      ),
      BagLocation(
        path = s3Path,
        bucket = "bucket",
      ),
      version,
      createdDate = Instant.now
    )
}
