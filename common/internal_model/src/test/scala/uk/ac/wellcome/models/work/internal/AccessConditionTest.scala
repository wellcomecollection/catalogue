package uk.ac.wellcome.models.work.internal

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class AccessConditionTest extends AnyFunSpec with Matchers {
  it("creates restricted access condition") {
    val restrictedValues = List(
      "Restricted",
      "Restricted access (Data Protection Act)",
      "Cannot Be Produced - View Digitised Version",
      "Certain restrictions apply.",
      "By Appointment.",
      "Restricted: currently undergoing conservation."
    )
    restrictedValues.foreach { str =>
      AccessStatus.apply(str) shouldBe Right(AccessStatus.Restricted)
    }
  }
  it("creates Unavailable access condition") {
    val restrictedValues = List("Missing.", "Temporarily Unavailable.")
    restrictedValues.foreach { str =>
      AccessStatus.apply(str) shouldBe Right(AccessStatus.Unavailable)
    }
  }
  it("creates PermissionRequired access condition") {
    val restrictedValues = List(
      "Permission Required.",
      "Donor Permission.",
      "Permission is required to view this item.")
    restrictedValues.foreach { str =>
      AccessStatus.apply(str) shouldBe Right(AccessStatus.PermissionRequired)
    }
  }

  it("creates the Open access condition") {
    AccessStatus("Unrestricted / Open.") shouldBe Right(AccessStatus.Open)
  }

  it("strips punctuation from access condition if present") {
    AccessStatus.apply("Open.") shouldBe Right(AccessStatus.Open)
  }

  it("errors if invalid AccessStatus") {
    val result = AccessStatus.apply("Oopsy")
    result shouldBe a[Left[_, _]]
    result.left.get shouldBe a[UnknownAccessStatus]
    result.left.get.getMessage shouldBe "Oopsy"
  }
}
