package uk.ac.wellcome.models.index

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.requests.common.VersionType.ExternalGte
import io.circe.Encoder
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Assertion, Suite}
import uk.ac.wellcome.elasticsearch.ElasticConfig
import uk.ac.wellcome.elasticsearch.model.CanonicalId
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil.toJson
import uk.ac.wellcome.models.work.internal.WorkState.Identified
import uk.ac.wellcome.models.work.internal.{Image, ImageState, Work, WorkState}

trait IndexFixtures extends ElasticsearchFixtures{ this: Suite =>
  def withLocalIndices[R](testWith: TestWith[ElasticConfig, R]): R =
    withLocalWorksIndex { worksIndex =>
      withLocalImagesIndex { imagesIndex =>
        testWith(ElasticConfig(worksIndex, imagesIndex))
      }
    }

  def withLocalWorksIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = IndexedWorkIndexConfig) { index =>
      testWith(index)
    }


  def withLocalMergedWorksIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = MergedWorkIndexConfig) { index =>
      testWith(index)
    }

  def withLocalDenormalisedWorksIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = DenormalisedWorkIndexConfig) {
      index =>
        testWith(index)
    }
  def withLocalInitialImagesIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = InitialImageIndexConfig) { index =>
      testWith(index)
    }

  def withLocalAugmentedImageIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = AugmentedImageIndexConfig) {
      index =>
        testWith(index)
    }

  def withLocalImagesIndex[R](testWith: TestWith[Index, R]): R =
    withLocalElasticsearchIndex[R](config = IndexedImageIndexConfig) { index =>
      testWith(index)
    }

  def assertElasticsearchEventuallyHasWork[State <: WorkState](
                                                                index: Index,
                                                                works: Work[State]*)(implicit enc: Encoder[Work[State]]): Seq[Assertion] = {
    implicit val id: CanonicalId[Work[State]] =
      (work: Work[State]) => work.id
    assertElasticsearchEventuallyHas(index, works: _*)
  }

  def assertElasticsearchEventuallyHasImage[State <: ImageState](
                                                                  index: Index,
                                                                  images: Image[State]*)(
                                                                  implicit enc: Encoder[Image[State]]): Seq[Assertion] = {
    implicit val id: CanonicalId[Image[State]] =
      (image: Image[State]) => image.id
    assertElasticsearchEventuallyHas(index, images: _*)
  }

  def assertElasticsearchNeverHasWork(index: Index,
                                      works: Work[Identified]*): Unit = {
    implicit val id: CanonicalId[Work[Identified]] =
      (work: Work[Identified]) => work.state.canonicalId
    assertElasticsearchNeverHas(index, works: _*)
  }

  def insertIntoElasticsearch[State <: WorkState](
                                                   index: Index,
                                                   works: Work[State]*)(implicit encoder: Encoder[Work[State]]): Assertion = {
    val result = elasticClient.execute(
      bulk(
        works.map { work =>
          val jsonDoc = toJson(work).get
          indexInto(index.name)
            .version(work.version)
            .versionType(ExternalGte)
            .id(work.id)
            .doc(jsonDoc)
        }
      ).refreshImmediately
    )

    // With a large number of works this can take a long time
    // 30 seconds should be enough
    whenReady(result, Timeout(Span(30, Seconds))) { _ =>
      getSizeOf(index) shouldBe works.size
    }
  }

  def insertImagesIntoElasticsearch[State <: ImageState](index: Index,
                                                         images: Image[State]*)(
                                                          implicit encoder: Encoder[Image[State]]): Assertion = {
    val result = elasticClient.execute(
      bulk(
        images.map { image =>
          val jsonDoc = toJson(image).get

          indexInto(index.name)
            .version(image.version)
            .versionType(ExternalGte)
            .id(image.id)
            .doc(jsonDoc)
        }
      ).refreshImmediately
    )

    whenReady(result) { _ =>
      getSizeOf(index) shouldBe images.size
    }
  }
  private def getSizeOf(index: Index): Long =
    elasticClient
      .execute { count(index.name) }
      .await
      .result
      .count
}
