package uk.ac.wellcome.pipeline_storage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.Index
import org.scalatest.matchers.should.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import io.circe.Encoder

import uk.ac.wellcome.elasticsearch.{IndexConfig, WorksAnalysis, NoStrictMapping}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.pipeline_storage.fixtures.{
  ElasticIndexerFixtures,
  SampleDocument,
  SampleDocumentData
}

class ElasticIndexerTest
    extends AnyFunSpec
    with ScalaFutures
    with Matchers
    with IdentifiersGenerators
    with ElasticsearchFixtures
    with ElasticIndexerFixtures {

  import SampleDocument._

  it("inserts a document into Elasticsearch") {
    val document = SampleDocument(1, createCanonicalId, "document")

    withIndexAndIndexer[SampleDocument, Any]() {
      case (index, indexer) =>
        val future = indexer.index(Seq(document))

        whenReady(future) { result =>
          result.right.get should contain(document)
          assertElasticsearchEventuallyHas(index = index, document)
        }
    }
  }

  it("only adds one record when the same ID is ingested multiple times") {
    val document = SampleDocument(1, createCanonicalId, "document")

    withIndexAndIndexer[SampleDocument, Any]() {
      case (index, indexer) =>
        val future = Future.sequence(
          (1 to 2).map(_ => indexer.index(Seq(document)))
        )

        whenReady(future) { _ =>
          assertElasticsearchEventuallyHas(index = index, document)
        }
    }
  }

  it("doesn't add a document with a lower version") {
    val document = SampleDocument(3, createCanonicalId, "document")
    val olderDocument = document.copy(version = 1)

    withIndexAndIndexer[SampleDocument, Any]() {
      case (index, indexer) =>
        val future = for {
          _ <- indexer.index(Seq(document))
          result <- indexer.index(Seq(olderDocument))
        } yield result

        whenReady(future) { result =>
          result.isRight shouldBe true
          assertElasticsearchEventuallyHas(index = index, document)
        }
    }
  }

  it("replaces a document with the same version") {
    val document = SampleDocument(3, createCanonicalId, "document")
    val updatedDocument = document.copy(
      title = "A different title"
    )

    withIndexAndIndexer[SampleDocument, Any]() {
      case (index, indexer) =>
        val future = for {
          _ <- indexer.index(Seq(document))
          result <- indexer.index(Seq(updatedDocument))
        } yield result

        whenReady(future) { result =>
          result.right.get should contain(updatedDocument)
          assertElasticsearchEventuallyHas(index = index, updatedDocument)
        }
    }
  }

  it("inserts a list of documents into elasticsearch and returns them") {
    val documents =
      (1 to 5).map(i => SampleDocument(1, createCanonicalId, f"document $i"))

    withIndexAndIndexer[SampleDocument, Any]() {
      case (index, indexer) =>
        val future = indexer.index(documents)

        whenReady(future) { successfullyInserted =>
          assertElasticsearchEventuallyHas(index = index, documents: _*)
          successfullyInserted.right.get should contain theSameElementsAs documents
        }
    }
  }

  it("returns a list of documents that weren't indexed correctly") {
    val validDocuments =
      (1 to 5).map(i => SampleDocument(1, createCanonicalId, f"document $i"))
    val notMatchingMappingDocuments = (1 to 3).map(
      i =>
        SampleDocument(
          1,
          createCanonicalId,
          f"document $i",
          SampleDocumentData(Some("blah bluh blih"))))
    val documents = validDocuments ++ notMatchingMappingDocuments

    withIndexAndIndexer[SampleDocument, Any](
      config = StrictWithNoDataIndexConfig) {
      case (index, indexer) =>
        val future = indexer.index(
          documents = documents
        )

        whenReady(future) { result =>
          assertElasticsearchEventuallyHas(index = index, validDocuments: _*)
          assertElasticsearchNeverHas(
            index = index,
            notMatchingMappingDocuments: _*)
          result.left.get should contain only (notMatchingMappingDocuments: _*)
        }
    }
  }

  def withIndexAndIndexer[T, R](config: IndexConfig = NoStrictMapping)(
    testWith: TestWith[(Index, Indexer[T]), R])(implicit encoder: Encoder[T],
                                                indexable: Indexable[T]) =
    withLocalElasticsearchIndex(config) { index =>
      withElasticIndexer[T, R](index) { indexer =>
        testWith((index, indexer))
      }
    }

  object StrictWithNoDataIndexConfig extends IndexConfig {
    import com.sksamuel.elastic4s.ElasticDsl._

    val analysis = WorksAnalysis()

    val title = textField("title")
    val data = objectField("data")

    val mapping = properties(Seq(title, canonicalId, version, data))
      .dynamic(DynamicMapping.Strict)
  }
}
