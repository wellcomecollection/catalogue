package uk.ac.wellcome.elasticsearch

import java.time.Instant

import org.scalacheck.ScalacheckShapeless._
import com.sksamuel.elastic4s.ElasticError
import org.scalacheck.Gen.chooseNum
import org.scalacheck.{Arbitrary, Shrink}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.json.utils.JsonAssertions
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.models.work.generators.{ImageGenerators, WorksGenerators}
import uk.ac.wellcome.models.work.internal._

class WorksIndexConfigTest
    extends AnyFunSpec
    with ElasticsearchFixtures
    with ScalaFutures
    with Eventually
    with Matchers
    with JsonAssertions
    with ScalaCheckPropertyChecks
    with WorksGenerators
    with ImageGenerators {

  // On failure, scalacheck tries to shrink to the smallest input that causes a failure.
  // With IdentifiedWork, that means that it never actually completes.
  implicit val noShrink = Shrink.shrinkAny[IdentifiedBaseWork]

  // We use this for the scalacheck of the java.time.Instant type
  // We could just import the library, but I might wait until we need more
  // Taken from here:
  // https://github.com/rallyhealth/scalacheck-ops/blob/master/core/src/main/scala/org/scalacheck/ops/time/ImplicitJavaTimeGenerators.scala
  implicit val arbInstant: Arbitrary[Instant] = {
    Arbitrary {
      for {
        millis <- chooseNum(
          Instant.MIN.getEpochSecond,
          Instant.MAX.getEpochSecond)
        nanos <- chooseNum(Instant.MIN.getNano, Instant.MAX.getNano)
      } yield {
        Instant.ofEpochMilli(millis).plusNanos(nanos)
      }
    }
  }

  it("puts a valid work") {
    forAll { sampleWork: IdentifiedBaseWork =>
      withLocalWorksIndex { index =>
        whenReady(indexObject(index, sampleWork)) { resp =>
          assertObjectIndexed(index, sampleWork)
        }
      }
    }
  }

  // Possibly because the number of variations in the work model is too big,
  // a bug in the mapping related to person subjects wasn't caught by the above test.
  // So let's add a specific one
  it("puts a work with a person subject") {
    withLocalWorksIndex { index =>
      val sampleWork = createIdentifiedWorkWith(
        subjects = List(
          Subject(
            id = Id.Unidentifiable,
            label = "Daredevil",
            concepts = List(
              Person(
                id = Id.Unidentifiable,
                label = "Daredevil",
                prefix = Some("Superhero"),
                numeration = Some("I")
              )
            )
          )
        )
      )
      whenReady(indexObject(index, sampleWork)) { _ =>
        assertObjectIndexed(index, sampleWork)
      }
    }
  }

  // Possibly because the number of variations in the work model is too big,
  // a bug in the mapping related to accessConditions wasn't caught by the catch-all test above.
  it("puts a work with a access condition") {
    val accessCondition: AccessCondition = AccessCondition(
      status = Some(AccessStatus.Open),
      terms = Some("ask nicely"),
      to = Some("2014"))

    withLocalWorksIndex { index =>
      val sampleWork = createIdentifiedWorkWith(
        items = List(
          createIdentifiedItemWith(locations = List(createDigitalLocationWith(
            accessConditions = List(accessCondition))))))
      whenReady(indexObject(index, sampleWork)) { _ =>
        assertObjectIndexed(index, sampleWork)
      }
    }
  }

  // Because we use copy_to and some other index functionality
  // the potentially fails at PUT index time, we urn this test
  // e.g. copy_to was previously set to `collection.depth`
  // which would not work as the mapping is strict and `collection`
  // only exists at the `data.collectionPath` level
  it("puts a work with a collection") {
    val collectionPath =
      Some(
        CollectionPath(
          path = "PATH/FOR/THE/COLLECTION",
          level = Some(CollectionLevel.Item),
          label = Some("PATH/FOR/THE/COLLECTION")))

    withLocalWorksIndex { index =>
      val sampleWork = createIdentifiedWorkWith(collectionPath = collectionPath)
      whenReady(indexObject(index, sampleWork)) { _ =>
        assertObjectIndexed(index, sampleWork)
      }
    }
  }

  it("can ingest a work with an image") {
    withLocalWorksIndex { index =>
      val sampleWork = createIdentifiedWorkWith(
        images = List(createUnmergedImage.toIdentified)
      )
      whenReady(indexObject(index, sampleWork)) { _ =>
        assertObjectIndexed(index, sampleWork)
      }
    }
  }

  it("does not put an invalid work") {
    withLocalWorksIndex { index =>
      val badTestObject = BadTestObject(
        id = "id",
        weight = 5
      )

      whenReady(indexObject(index, badTestObject)) { response =>
        response.isError shouldBe true
        response.error shouldBe a[ElasticError]
      }
    }
  }
}
