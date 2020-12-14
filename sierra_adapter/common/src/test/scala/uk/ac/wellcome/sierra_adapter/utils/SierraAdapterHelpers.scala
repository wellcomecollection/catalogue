package uk.ac.wellcome.sierra_adapter.utils

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.sierra_adapter.model.SierraTransformable
import uk.ac.wellcome.storage.Version
import uk.ac.wellcome.storage.maxima.memory.MemoryMaxima
import uk.ac.wellcome.storage.store.VersionedStore
import uk.ac.wellcome.storage.store.memory.{MemoryStore, MemoryVersionedStore}
import uk.ac.wellcome.json.JsonUtil._
import weco.catalogue.source_model.store.SourceVHS

trait SierraAdapterHelpers extends Matchers {
  type SierraVHS = VersionedStore[String, Int, SierraTransformable]

  def createStore[T](
    data: Map[Version[String, Int], T] = Map[Version[String, Int], T]())
    : MemoryVersionedStore[String, T] =
    new MemoryVersionedStore(new MemoryStore(data) with MemoryMaxima[String, T])

  def assertStored[T](id: String,
                      t: T,
                      sourceVHS: SourceVHS[T]): Assertion =
    sourceVHS.underlying.getLatest(id).right.get.identifiedT shouldBe t

  def assertStoredAndSent[T](id: Version[String, Int],
                             t: T,
                             sourceVHS: SourceVHS[T],
                             messageSender: MemoryMessageSender): Assertion = {
    assertStored(id.id, t, sourceVHS)
    messageSender.getMessages[Version[String, Int]] should contain(id)
  }
}
