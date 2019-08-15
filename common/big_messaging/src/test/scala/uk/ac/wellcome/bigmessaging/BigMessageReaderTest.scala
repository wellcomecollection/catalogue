package uk.ac.wellcome.bigmessaging

import io.circe.Decoder
import org.scalatest.{EitherValues, FunSpec, Matchers}
import uk.ac.wellcome.bigmessaging.memory.MemoryTypedStoreCompanion
import uk.ac.wellcome.bigmessaging.message.{
  InlineNotification,
  RemoteNotification
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.exceptions.JsonDecodingError
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.generators.ObjectLocationGenerators
import uk.ac.wellcome.storage.store.{TypedStore, TypedStoreEntry}
import scala.util.{Failure, Success}

class BigMessageReaderTest
    extends FunSpec
    with Matchers
    with EitherValues
    with ObjectLocationGenerators {
  case class Shape(colour: String, sides: Int)

  val blueTriangle = Shape(colour = "blue", sides = 3)

  def createReader(typedStoreShape: TypedStore[ObjectLocation, Shape] =
                     MemoryTypedStoreCompanion[ObjectLocation, Shape]())(
    implicit decoderS: Decoder[Shape]): BigMessageReader[Shape] =
    new BigMessageReader[Shape] {
      override val typedStore: TypedStore[ObjectLocation, Shape] =
        typedStoreShape
      override implicit val decoder: Decoder[Shape] = decoderS
    }

  it("reads a large message from the object store") {
    val store = MemoryTypedStoreCompanion[ObjectLocation, Shape]()
    val reader = createReader(store)
    val objectLocation = createObjectLocation

    store.put(objectLocation)(TypedStoreEntry(blueTriangle, Map.empty))

    val notification = RemoteNotification(objectLocation)

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

    val notification = RemoteNotification(
      location = ObjectLocation("does-not", "exist")
    )

    val result = reader.read(notification)

    result shouldBe a[Failure[_]]
    val err = result.failed.get
    err shouldBe a[Throwable]
    err.getMessage shouldBe "Nothing at does-not/exist"
  }
}
