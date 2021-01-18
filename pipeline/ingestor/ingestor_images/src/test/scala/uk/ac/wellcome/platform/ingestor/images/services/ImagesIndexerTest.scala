package uk.ac.wellcome.platform.ingestor.images.services

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.elasticsearch.IndexedImageIndexConfig
import uk.ac.wellcome.elasticsearch.test.fixtures.ElasticsearchFixtures
import uk.ac.wellcome.models.work.generators.ImageGenerators
import uk.ac.wellcome.models.work.internal.{Image, ImageState}
import uk.ac.wellcome.pipeline_storage.ElasticIndexer
import uk.ac.wellcome.pipeline_storage.Indexable.imageIndexable
import uk.ac.wellcome.models.Implicits._
import uk.ac.wellcome.pipeline_storage.fixtures.ElasticIndexerFixtures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ImagesIndexerTest
    extends AnyFunSpec
    with Matchers
    with ElasticsearchFixtures
    with ScalaFutures
    with ElasticIndexerFixtures
    with ImageGenerators {

  it("ingests an image") {
    withLocalImagesIndex { index =>
      val imagesIndexer =
        new ElasticIndexer[Image[ImageState.Augmented]](
          elasticClient,
          index,
          IndexedImageIndexConfig)
      val image = createImageData.toAugmentedImage
      whenReady(imagesIndexer(List(image))) { r =>
        r.isRight shouldBe true
        r.right.get shouldBe List(image)
        assertElasticsearchEventuallyHas(index = index, image)
      }
    }
  }

  it("ingests a list of images") {
    withLocalImagesIndex { index =>
      val imagesIndexer =
        new ElasticIndexer[Image[ImageState.Augmented]](
          elasticClient,
          index,
          IndexedImageIndexConfig)
      val images = (1 to 5).map(_ => createImageData.toAugmentedImage)
      whenReady(imagesIndexer(images)) { r =>
        r.isRight shouldBe true
        r.right.get should contain theSameElementsAs images
        assertElasticsearchEventuallyHas(index = index, images: _*)
      }
    }
  }

  it("ingests a higher version of the same image") {
    withLocalImagesIndex { index =>
      val imagesIndexer =
        new ElasticIndexer[Image[ImageState.Augmented]](
          elasticClient,
          index,
          IndexedImageIndexConfig)
      val image = createImageData.toAugmentedImage
      val newerImage =
        image.copy(
          modifiedTime = image.modifiedTime + (2 minutes)
        )
      val result = for {
        _ <- imagesIndexer(List(image))
        res <- imagesIndexer(List(newerImage))
      } yield res
      whenReady(result) { r =>
        r.isRight shouldBe true
        r.right.get shouldBe List(newerImage)
        assertElasticsearchEventuallyHas(index = index, newerImage)
      }
    }
  }

  it("doesn't replace a newer version with a lower one") {
    withLocalImagesIndex { index =>
      val imagesIndexer =
        new ElasticIndexer[Image[ImageState.Augmented]](
          elasticClient,
          index,
          IndexedImageIndexConfig)
      val image = createImageData.toAugmentedImage
      val olderImage =
        image.copy(
          modifiedTime = image.modifiedTime - (2 minutes)
        )
      val result = for {
        _ <- imagesIndexer(List(image))
        res <- imagesIndexer(List(olderImage))
      } yield res
      whenReady(result) { r =>
        r.isRight shouldBe true
        r.right.get shouldBe List(olderImage)
        assertElasticsearchEventuallyHas(index = index, image)
      }
    }
  }
}
