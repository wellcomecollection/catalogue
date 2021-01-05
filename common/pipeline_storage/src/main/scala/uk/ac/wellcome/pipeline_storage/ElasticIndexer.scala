package uk.ac.wellcome.pipeline_storage

import scala.concurrent.{ExecutionContext, Future}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.bulk.{BulkResponse, BulkResponseItem}
import com.sksamuel.elastic4s.requests.common.VersionType.ExternalGte
import com.sksamuel.elastic4s.{ElasticClient, Index, Response}
import com.sksamuel.elastic4s.{Indexable => ElasticIndexable}
import io.circe.{Encoder, Printer}
import grizzled.slf4j.Logging

import uk.ac.wellcome.elasticsearch.{ElasticsearchIndexCreator, IndexConfig}

class ElasticIndexer[T: Indexable](
  client: ElasticClient,
  index: Index,
  config: IndexConfig)(implicit ec: ExecutionContext, encoder: Encoder[T])
    extends Indexer[T]
    with Logging {

  implicit val elasticIndexable = new ElasticIndexable[T] {
    override def json(item: T): String = {
      // Here we drop nulls in the indexed json to reduce storage size: this is
      // mainly for relations which only contain very limited work data.
      // In circe v0.14.0 we may also consider adding dropEmptyValues, which is
      // a new Json method that is non trivial to include here
      val json = encoder(item).deepDropNullValues
      Printer.noSpaces.print(json)
    }
  }

  final def init(): Future[Unit] =
    new ElasticsearchIndexCreator(client, index, config).create

  final def apply(documents: Seq[T]): Future[Either[Seq[T], Seq[T]]] = {
    assert(documents.nonEmpty, "Cannot index an empty list of documents")

    debug(s"Indexing ${documents.map(doc => indexable.id(doc)).mkString(", ")}")

    val inserts = documents.map { document =>
      indexInto(index.name)
        .version(indexable.version(document))
        .versionType(ExternalGte)
        .id(indexable.id(document))
        .doc(document)
    }

    client
      .execute {
        bulk(inserts)
      }
      .map { response: Response[BulkResponse] =>
        if (response.isError) {
          error(s"Error from Elasticsearch: $response")
          Left(documents)
        } else {
          debug(s"Bulk response = $response")
          val bulkResponse = response.result
          val actualFailures = bulkResponse.failures.filterNot {
            isVersionConflictException
          }

          if (actualFailures.nonEmpty) {
            val failedIds = actualFailures.map { failure =>
              error(s"Failed ingesting ${failure.id}: ${failure.error}")
              failure.id
            }

            Left(documents.filter(doc => {
              failedIds.contains(indexable.id(doc))
            }))
          } else Right(documents)
        }
      }
  }

  /** Did we try to PUT a document with a lower version than the existing version?
    *
    */
  private def isVersionConflictException(
    bulkResponseItem: BulkResponseItem): Boolean = {
    // This error is returned by Elasticsearch when we try to PUT a document
    // with a lower version than the existing version.
    val alreadyIndexedHasHigherVersion = bulkResponseItem.error
      .exists(bulkError =>
        bulkError.`type`.contains("version_conflict_engine_exception"))

    if (alreadyIndexedHasHigherVersion) {
      info(
        s"Skipping ${bulkResponseItem.id} because already indexed item has a higher version (${bulkResponseItem.error}")
    }

    alreadyIndexedHasHigherVersion
  }

}
