package uk.ac.wellcome.pipeline_storage.elastic

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{Index, Response}
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import org.scalatest.Assertion
import uk.ac.wellcome.elasticsearch.{
  IndexConfig,
  IndexConfigFields,
  NoStrictMapping,
  WorksAnalysis
}
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil.toJson
import uk.ac.wellcome.pipeline_storage.fixtures.{
  SampleDocument,
  SampleDocumentData
}
import uk.ac.wellcome.pipeline_storage.{
  ElasticIndexer,
  Indexer,
  IndexerTestCases
}

import scala.concurrent.ExecutionContext.Implicits.global

class ElasticIndexerTest
    extends IndexerTestCases[Index, SampleDocument]
    with ElasticsearchFixtures {

  import SampleDocument._

  override def withContext[R](documents: Seq[SampleDocument])(
    testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex(config = NoStrictMapping) { implicit index =>
      if (documents.nonEmpty) {
        withIndexer { indexer =>
          indexer(documents).await shouldBe a[Right[_, _]]
        }
      }

      documents.foreach { doc =>
        assertObjectIndexed(index, doc)
      }

      testWith(index)
    }

  override def withIndexer[R](testWith: TestWith[Indexer[SampleDocument], R])(
    implicit index: Index): R = {
    val indexer = new ElasticIndexer[SampleDocument](
      client = elasticClient,
      index = index,
      config = NoStrictMapping
    )

    testWith(indexer)
  }

  override def createDocumentWith(id: String, version: Int): SampleDocument =
    SampleDocument(canonicalId = id, version = version, title = s"$id:$version")

  override def assertIsIndexed(doc: SampleDocument)(
    implicit index: Index): Assertion =
    assertElasticsearchEventuallyHas(index, doc).head

  override def assertIsNotIndexed(doc: SampleDocument)(
    implicit index: Index): Assertion = {
    val documentJson = toJson(doc).get

    eventually {
      val response: Response[GetResponse] = elasticClient.execute {
        get(index, canonicalId.canonicalId(doc))
      }.await

      val getResponse = response.result

      // If there's a document with this ID, we want to make sure it's something
      // different.  If there's no document with this ID, then all is well.
      if (getResponse.exists) {
        assertJsonStringsAreDifferent(getResponse.sourceAsString, documentJson)
      } else {
        assert(true)
      }
    }
  }

  it("returns a list of documents that weren't indexed correctly") {
    val validDocuments = (1 to 5).map { _ =>
      createDocument
    }
    val invalidDocuments = (1 to 3).map { _ =>
      createDocument
        .copy(data = SampleDocumentData(genre = Some(randomAlphanumeric())))
    }

    object StrictWithNoDataIndexConfig
        extends IndexConfig
        with IndexConfigFields {

      import com.sksamuel.elastic4s.ElasticDsl._

      val analysis = WorksAnalysis()

      val title = textField("title")
      val data = objectField("data")

      val mapping = properties(Seq(title, canonicalId, version, data))
        .dynamic(DynamicMapping.Strict)
    }

    withLocalElasticsearchIndex(config = StrictWithNoDataIndexConfig) {
      implicit index =>
        withIndexer { indexer =>
          val future = indexer(validDocuments ++ invalidDocuments)

          whenReady(future) { result =>
            result.left.get should contain only (invalidDocuments: _*)

            validDocuments.foreach { doc =>
              assertIsIndexed(doc)
            }

            invalidDocuments.foreach { doc =>
              assertIsNotIndexed(doc)
            }
          }
        }
    }
  }

  it("does not store optional fields when those fields are unmapped") {

    val documents = List(
      createDocumentWith("A", 1).copy(
        data = SampleDocumentData(genre = Some("Crime"))
      ),
      createDocumentWith("B", 2).copy(
        data = SampleDocumentData(date = Some("10/10/2010"))
      ),
    )

    object UnmappedDataMappingIndexConfig
        extends IndexConfig
        with IndexConfigFields {

      import com.sksamuel.elastic4s.ElasticDsl._

      val analysis = WorksAnalysis()

      val title = textField("title")
      val data = objectField("data").dynamic("false")

      val mapping = properties(Seq(title, canonicalId, version, data))
        .dynamic(DynamicMapping.Strict)
    }

    withLocalElasticsearchIndex(config = UnmappedDataMappingIndexConfig) {
      implicit index =>
        withIndexer { indexer =>
          val future = indexer(documents)

          whenReady(future) { result =>
            result.right.get should contain only (documents: _*)
            val hits = eventually {
              val response = elasticClient.execute {
                search(index).matchAllQuery()
              }.await

              val hits = response.result.hits.hits

              hits should have size 2
              hits
            }
            hits.map(_.sourceAsMap).toList shouldBe List(
              Map(
                "canonicalId" -> "A",
                "version" -> 1,
                "title" -> "A:1",
                "data" -> Map("genre" -> "Crime")
              ),
              Map(
                "canonicalId" -> "B",
                "version" -> 2,
                "title" -> "B:2",
                "data" -> Map("date" -> "10/10/2010")
              )
            )
          }
        }
    }
  }

  it("returns a failed future if indexing an empty list of ids"){

    withContext() { implicit context =>
      withIndexer {
        indexer =>
        val future = indexer(Seq())

        whenReady(future.failed) { ex =>
          ex shouldBe a [IllegalArgumentException]
        }
      }
    }
  }
}
