package uk.ac.wellcome.platform.snapshot_generator

import java.io.File

import com.amazonaws.services.s3.model.GetObjectRequest
import com.sksamuel.elastic4s.Index
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.display.models.{ApiVersions, DisplaySerialisationTestBase}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.fixtures.{SNS, SQS}
import uk.ac.wellcome.models.work.generators.WorksGenerators
import uk.ac.wellcome.platform.snapshot_generator.fixtures.WorkerServiceFixture
import uk.ac.wellcome.platform.snapshot_generator.models.{
  CompletedSnapshotJob,
  SnapshotJob
}
import uk.ac.wellcome.platform.snapshot_generator.test.utils.GzipUtils
import uk.ac.wellcome.storage.fixtures.S3Fixtures
import uk.ac.wellcome.storage.fixtures.S3Fixtures.Bucket

class SnapshotGeneratorFeatureTest
    extends AnyFunSpec
    with Eventually
    with Matchers
    with Akka
    with S3Fixtures
    with SNS
    with SQS
    with GzipUtils
    with JsonAssertions
    with IntegrationPatience
    with DisplaySerialisationTestBase
    with WorkerServiceFixture
    with WorksGenerators {

  it("completes a snapshot generation") {
    withFixtures {
      case (queue, topic, worksIndex, _, publicBucket: Bucket) =>
        val works = createIdentifiedWorks(count = 3)

        insertIntoElasticsearch(worksIndex, works: _*)

        val publicObjectKey = "target.txt.gz"

        val snapshotJob = SnapshotJob(
          publicBucketName = publicBucket.name,
          publicObjectKey = publicObjectKey,
          apiVersion = ApiVersions.v2
        )

        sendNotificationToSQS(queue = queue, message = snapshotJob)

        eventually {
          val downloadFile =
            File.createTempFile("snapshotGeneratorFeatureTest", ".txt.gz")

          s3Client.getObject(
            new GetObjectRequest(publicBucket.name, publicObjectKey),
            downloadFile)

          val actualJsonLines: List[String] =
            readGzipFile(downloadFile.getPath).split("\n").toList

          val expectedJsonLines = works.sortBy { _.canonicalId }.map { work =>
            s"""
              |{
              |  "id": "${work.canonicalId}",
              |  "title": "${work.data.title.get}",
              |  "identifiers": [ ${identifier(work.sourceIdentifier)} ],
              |  "contributors": [ ],
              |  "genres": [ ],
              |  "subjects": [ ],
              |  "items": [ ],
              |  "production": [ ],
              |  "alternativeTitles": [ ],
              |  "notes": [ ],
              |  "images": [ ],
              |  "type": "Work"
              |}""".stripMargin
          }

          actualJsonLines.sorted.zip(expectedJsonLines).foreach {
            case (actualLine, expectedLine) =>
              println(s"actualLine = <<$actualLine>>")
              assertJsonStringsAreEqual(actualLine, expectedLine)
          }

          val receivedMessages = listMessagesReceivedFromSNS(topic)
          receivedMessages.size should be >= 1

          val expectedJob = CompletedSnapshotJob(
            snapshotJob = snapshotJob,
            targetLocation =
              s"http://localhost:33333/${publicBucket.name}/$publicObjectKey"
          )
          val actualJob =
            fromJson[CompletedSnapshotJob](receivedMessages.head.message).get
          actualJob shouldBe expectedJob
        }
    }

  }

  def withFixtures[R](
    testWith: TestWith[(Queue, Topic, Index, Index, Bucket), R]): R =
    withActorSystem { implicit actorSystem =>
      withLocalSqsQueue() { queue =>
        withLocalSnsTopic { topic =>
          withLocalWorksIndex { worksIndex =>
            withLocalS3Bucket { bucket =>
              withWorkerService(queue, topic, worksIndex) { _ =>
                testWith((queue, topic, worksIndex, worksIndex, bucket))
              }
            }
          }
        }
      }
    }
}
