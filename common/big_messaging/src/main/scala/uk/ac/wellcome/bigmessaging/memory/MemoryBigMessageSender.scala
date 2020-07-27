package uk.ac.wellcome.bigmessaging.memory

import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.bigmessaging.message.{
  InlineNotification,
  MemoryRemoteNotification,
  RemoteNotification
}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.storage.providers.memory.MemoryLocation
import uk.ac.wellcome.storage.store.Store
import uk.ac.wellcome.storage.store.memory.MemoryStore

class MemoryBigMessageSender[T](
  maxSize: Int = Int.MaxValue,
  storeNamespace: String = "MemoryBigMessageSender",
  messageDestination: String = "MemoryBigMessageSender"
)(
  implicit
  val encoder: Encoder[T],
) extends BigMessageSender[MemoryLocation, String, T] {

  override val messageSender: MemoryMessageSender = new MemoryMessageSender {
    override val destination: String = messageDestination
  }

  override val store: Store[MemoryLocation, T] =
    new MemoryStore(initialEntries = Map.empty)

  override val namespace: String = storeNamespace
  override val maxMessageSize: Int = maxSize

  def messages: List[messageSender.underlying.MemoryMessage] =
    messageSender.messages

  def getMessages[S]()(implicit decoder: Decoder[S]): Seq[S] =
    messageSender
      .getMessages[InlineNotification]()
      .map { _.jsonString }
      .map { fromJson[S](_).get }

  override def createLocation(namespace: String, key: String): MemoryLocation =
    MemoryLocation(namespace = namespace, path = key)

  override def createNotification(
    location: MemoryLocation): RemoteNotification[MemoryLocation] =
    MemoryRemoteNotification(location)
}
