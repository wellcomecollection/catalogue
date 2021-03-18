package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Suite}
import uk.ac.wellcome.platform.transformer.miro.MiroRecordTransformer
import uk.ac.wellcome.platform.transformer.miro.exceptions.MiroTransformerException
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import weco.catalogue.internal_model.work.Work
import weco.catalogue.internal_model.work.WorkState.Source

import scala.util.Try

trait MiroTransformableWrapper extends Matchers { this: Suite =>
  val transformer = new MiroRecordTransformer

  def transformWork(miroRecord: MiroRecord): Work.Visible[Source] = {
    val triedWork: Try[Work[Source]] =
      transformer.transform(
        miroRecord = miroRecord,
        miroMetadata = MiroMetadata(isClearedForCatalogueAPI = true),
        version = 1
      )

    if (triedWork.isFailure) {
      triedWork.failed.get.printStackTrace()
      println(
        triedWork.failed.get
          .asInstanceOf[MiroTransformerException]
          .e
          .getMessage)
    }

    triedWork.isSuccess shouldBe true
    triedWork.get.asInstanceOf[Work.Visible[Source]]
  }

  def assertTransformWorkFails(miroRecord: MiroRecord): Assertion =
    transformer
      .transform(
        miroRecord = miroRecord,
        miroMetadata = MiroMetadata(isClearedForCatalogueAPI = true),
        version = 1
      )
      .isSuccess shouldBe false
}
