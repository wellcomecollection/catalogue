package uk.ac.wellcome.bigmessaging

import io.circe.Decoder
import org.scalatest.EitherValues
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.bigmessaging.message.{InlineNotification, MemoryRemoteNotification}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.storage.generators.MemoryLocationGenerators
import uk.ac.wellcome.storage.providers.memory.MemoryLocation
import uk.ac.wellcome.storage.store.Store
import uk.ac.wellcome.storage.store.memory.MemoryStore

import scala.util.{Failure, Success}

class BigMessageReaderTest
    extends AnyFunSpec
    with Matchers
    with EitherValues
    with MemoryLocationGenerators {
  case class Shape(colour: String, sides: Int)

  val blueTriangle = Shape(colour = "blue", sides = 3)

  def createReader(shapeStore: Store[MemoryLocation, Shape] = new MemoryStore(
                     Map.empty))(
    implicit decoderS: Decoder[Shape]): BigMessageReader[MemoryLocation, Shape] =
    new BigMessageReader[MemoryLocation, Shape] {
      override val store: Store[MemoryLocation, Shape] =
        shapeStore
      override implicit val decoder: Decoder[Shape] = decoderS
    }

  it("reads a large message from the object store") {
    val store = new MemoryStore(Map.empty[MemoryLocation, Shape])
    val reader = createReader(store)
    val objectLocation = createMemoryLocation

    store.put(objectLocation)(blueTriangle)

    val notification = MemoryRemoteNotification(objectLocation)

    reader.read(notification) shouldBe Success(blueTriangle)
  }

  it("reads an inline notification") {
    val reader = createReader()

    val notification = InlineNotification(toJson(blueTriangle).get)

    reader.read(notification) shouldBe Success(blueTriangle)
  }

  it("fails if the inline notification contains malformed JSON") {
    val reader = createReader()

    val notification = InlineNotification("xyz")

    val result = reader.read(notification)

    result shouldBe a[Failure[_]]
    val err = result.failed.get
    err shouldBe a[JsonDecodingError]
  }

  it("fails if the remote notification refers to a non-existent location") {
    val reader = createReader()

    val notification = MemoryRemoteNotification(location = MemoryLocation("does-not", "exist"))

    val result = reader.read(notification)

    result shouldBe a[Failure[_]]
    val err = result.failed.get
    err shouldBe a[Throwable]
    err.getMessage shouldBe "Nothing at does-not/exist"
  }
}
