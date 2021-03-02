package weco.catalogue.sierra_merger

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.scalatest.{Assertion, EitherValues}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.storage.{Identified, Version}
import weco.catalogue.sierra_adapter.generators.SierraGenerators
import weco.catalogue.sierra_adapter.models._
import weco.catalogue.sierra_adapter.models.Implicits._
import weco.catalogue.sierra_merger.fixtures.RecordMergerFixtures
import weco.catalogue.sierra_merger.models.TransformableOps
import weco.catalogue.sierra_merger.services.Worker
import weco.catalogue.source_model.fixtures.SourceVHSFixture
import weco.catalogue.source_model.store.SourceVHS

trait SierraRecordMergerFeatureTestCases[Record <: AbstractSierraRecord[_]]
    extends AnyFunSpec
    with EitherValues
    with SQS
    with SourceVHSFixture
    with SierraGenerators
    with Eventually
    with IntegrationPatience {

  def withWorker[R](queue: Queue,
                    sourceVHS: SourceVHS[SierraTransformable] =
                      createSourceVHS[SierraTransformable])(
    testWith: TestWith[(Worker[Record, String], MemoryMessageSender), R]): R

  def createRecordWith(bibIds: List[SierraBibNumber]): Record

  implicit val encoder: Encoder[Record]
  implicit val transformableOps: TransformableOps[Record]

  def assertStoredAndSent(
    id: Version[String, Int],
    transformable: SierraTransformable,
    sourceVHS: SourceVHS[SierraTransformable],
    messageSender: MemoryMessageSender
  ): Assertion = {
    sourceVHS.underlying.get(id).value shouldBe Identified(id, transformable)

    messageSender.getMessages[Version[String, Int]] should contain(id)
  }

  it("stores a record from SQS") {
    withLocalSqsQueue() { queue =>
      val bibId = createSierraBibNumber
      val record = createRecordWith(bibIds = List(bibId))

      val sourceVHS = createSourceVHS[SierraTransformable]

      withWorker(queue, sourceVHS) {
        case (_, messageSender) =>
          sendNotificationToSQS(queue = queue, record)

          val expectedSierraTransformable =
            transformableOps.create(bibId, record)

          eventually {
            assertStoredAndSent(
              Version(
                expectedSierraTransformable.sierraId.withoutCheckDigit,
                0),
              expectedSierraTransformable,
              sourceVHS,
              messageSender
            )
          }
      }
    }
  }

  it("stores multiple items from SQS") {
    withLocalSqsQueue() { queue =>
      val bibId1 = createSierraBibNumber
      val record1 = createRecordWith(bibIds = List(bibId1))

      val bibId2 = createSierraBibNumber
      val record2 = createRecordWith(bibIds = List(bibId2))

      val sourceVHS = createSourceVHS[SierraTransformable]

      withWorker(queue, sourceVHS) {
        case (_, messageSender) =>
          sendNotificationToSQS(queue, record1)
          sendNotificationToSQS(queue, record2)

          eventually {
            val expectedSierraTransformable1 =
              transformableOps.create(bibId1, record1)

            val expectedSierraTransformable2 =
              transformableOps.create(bibId2, record2)

            assertStoredAndSent(
              Version(bibId1.withoutCheckDigit, 0),
              expectedSierraTransformable1,
              sourceVHS,
              messageSender
            )
            assertStoredAndSent(
              Version(bibId2.withoutCheckDigit, 0),
              expectedSierraTransformable2,
              sourceVHS,
              messageSender
            )
          }
      }
    }
  }

  val recordCanBeLinkedToMultipleBibs: Boolean = true

  it("sends a notification for every transformable which changes") {
    if (recordCanBeLinkedToMultipleBibs) {
      withLocalSqsQueue() { queue =>
        val bibIds = createSierraBibNumbers(3)
        val record = createRecordWith(bibIds = bibIds)

        val sourceVHS = createSourceVHS[SierraTransformable]

        withWorker(queue, sourceVHS) {
          case (_, messageSender) =>
            sendNotificationToSQS(queue = queue, record)

            val expectedTransformables = bibIds.map { bibId =>
              transformableOps.create(bibId, record)
            }

            eventually {
              expectedTransformables.map { tranformable =>
                assertStoredAndSent(
                  Version(tranformable.sierraId.withoutCheckDigit, 0),
                  tranformable,
                  sourceVHS,
                  messageSender
                )
              }
            }
        }
      }
    }
  }
}

class SierraBibRecordMergerFeatureTest
    extends SierraRecordMergerFeatureTestCases[SierraBibRecord]
    with RecordMergerFixtures {
  override def withWorker[R](queue: Queue,
                             sourceVHS: SourceVHS[SierraTransformable])(
    testWith: TestWith[(Worker[SierraBibRecord, String], MemoryMessageSender),
                       R]): R =
    withRunningWorker[SierraBibRecord, R](queue, sourceVHS) {
      testWith
    }

  override val recordCanBeLinkedToMultipleBibs = false

  override def createRecordWith(
    bibIds: List[SierraBibNumber]): SierraBibRecord =
    createSierraBibRecordWith(id = bibIds.head)

  override implicit val encoder: Encoder[SierraBibRecord] = deriveEncoder

  override implicit val transformableOps: TransformableOps[SierraBibRecord] =
    TransformableOps.bibTransformableOps
}

class SierraItemRecordMergerFeatureTest
    extends SierraRecordMergerFeatureTestCases[SierraItemRecord]
    with RecordMergerFixtures {
  override def withWorker[R](queue: Queue,
                             sourceVHS: SourceVHS[SierraTransformable])(
    testWith: TestWith[(Worker[SierraItemRecord, String], MemoryMessageSender),
                       R]): R =
    withRunningWorker[SierraItemRecord, R](queue, sourceVHS) {
      testWith
    }

  override def createRecordWith(
    bibIds: List[SierraBibNumber]): SierraItemRecord =
    createSierraItemRecordWith(bibIds = bibIds)

  override implicit val encoder: Encoder[SierraItemRecord] = deriveEncoder

  override implicit val transformableOps: TransformableOps[SierraItemRecord] =
    TransformableOps.itemTransformableOps
}

class SierraHoldingsRecordMergerFeatureTest
    extends SierraRecordMergerFeatureTestCases[SierraHoldingsRecord]
    with RecordMergerFixtures {
  override def withWorker[R](queue: Queue,
                             sourceVHS: SourceVHS[SierraTransformable])(
    testWith: TestWith[(Worker[SierraHoldingsRecord, String],
                        MemoryMessageSender),
                       R]): R =
    withRunningWorker[SierraHoldingsRecord, R](queue, sourceVHS) {
      testWith
    }

  override def createRecordWith(
    bibIds: List[SierraBibNumber]): SierraHoldingsRecord =
    createSierraHoldingsRecordWith(bibIds = bibIds)

  override implicit val encoder: Encoder[SierraHoldingsRecord] = deriveEncoder

  override implicit val transformableOps
    : TransformableOps[SierraHoldingsRecord] =
    TransformableOps.holdingsTransformableOps
}