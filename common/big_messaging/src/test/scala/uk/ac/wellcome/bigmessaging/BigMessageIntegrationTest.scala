package uk.ac.wellcome.bigmessaging

import io.circe.Decoder
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.bigmessaging.memory.MemoryBigMessageSender
import uk.ac.wellcome.bigmessaging.message.{
  InlineNotification,
  RemoteNotification
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.store.TypedStore

import scala.util.Success

class BigMessageIntegrationTest extends FunSpec with Matchers {
  case class Shape(colour: String, sides: Int)

  val yellowPentagon = Shape(colour = "yellow", sides = 5)

  def createPair(maxSize: Int)(implicit decoderS: Decoder[Shape])
    : (MemoryBigMessageSender[Shape], BigMessageReader[Shape]) = {
    val sender = new MemoryBigMessageSender[Shape](maxSize = maxSize)
    val reader = new BigMessageReader[Shape] {
      override val typedStore: TypedStore[ObjectLocation, Shape] =
        sender.typedStore
      override implicit val decoder: Decoder[Shape] = decoderS
    }

    (sender, reader)
  }

  it("handles an inline notification") {
    val (sender, reader) = createPair(maxSize = 1000)

    val notification = sender.sendT(yellowPentagon).get
    notification shouldBe a[InlineNotification]
    reader.read(notification) shouldBe Success(yellowPentagon)
  }

  it("handles a remote message") {
    val (sender, reader) = createPair(maxSize = 1)

    val notification = sender.sendT(yellowPentagon).get
    notification shouldBe a[RemoteNotification]
    reader.read(notification) shouldBe Success(yellowPentagon)
  }
}
