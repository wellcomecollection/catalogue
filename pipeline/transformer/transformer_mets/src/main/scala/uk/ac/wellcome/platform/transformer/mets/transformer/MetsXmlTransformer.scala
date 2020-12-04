package uk.ac.wellcome.platform.transformer.mets.transformer

import uk.ac.wellcome.storage.store.Readable
import uk.ac.wellcome.storage.Identified
import uk.ac.wellcome.mets_adapter.models.MetsSourceData
import uk.ac.wellcome.models.work.internal.{Work, WorkState}
import uk.ac.wellcome.models.work.internal.result.Result
import uk.ac.wellcome.storage.s3.S3ObjectLocation
import uk.ac.wellcome.transformer.common.worker.Transformer

class MetsXmlTransformer(store: Readable[S3ObjectLocation, String])
    extends Transformer[MetsSourceData] {

  override def apply(metsLocation: MetsSourceData,
                     version: Int): Result[Work[WorkState.Source]] =
    for {
      metsData <- transform(metsLocation)
      work <- metsData.toWork(
        metsLocation.version,
        modifiedTime = metsLocation.createdDate)
    } yield work

  def transform(metsLocation: MetsSourceData): Result[MetsData] = {
    metsLocation.deleted match {
      case true =>
        for {
          xml <- getMetsXml(metsLocation.xmlLocation)
          recordIdentifier <- xml.recordIdentifier
        } yield MetsData(recordIdentifier = recordIdentifier, deleted = true)
      case false =>
        getMetsXml(metsLocation.xmlLocation)
          .flatMap { root =>
            if (metsLocation.manifestations.isEmpty)
              transformWithoutManifestations(root)
            else
              transformWithManifestations(
                root,
                metsLocation.manifestationLocations)
          }
    }

  }

  private def transformWithoutManifestations(root: MetsXml): Result[MetsData] =
    for {
      id <- root.recordIdentifier
      accessConditionDz <- root.accessConditionDz
      accessConditionStatus <- root.accessConditionStatus
      accessConditionUsage <- root.accessConditionUsage
    } yield
      MetsData(
        recordIdentifier = id,
        accessConditionDz = accessConditionDz,
        accessConditionStatus = accessConditionStatus,
        accessConditionUsage = accessConditionUsage,
        fileReferencesMapping = root.fileReferencesMapping(id),
        titlePageId = root.titlePageId,
      )

  private def transformWithManifestations(
    root: MetsXml,
    manifestations: List[S3ObjectLocation]): Result[MetsData] =
    for {
      id <- root.recordIdentifier
      firstManifestation <- getFirstManifestation(root, manifestations)
      accessConditionDz <- firstManifestation.accessConditionDz
      accessConditionStatus <- firstManifestation.accessConditionStatus
      accessConditionUsage <- firstManifestation.accessConditionUsage
    } yield
      MetsData(
        recordIdentifier = id,
        accessConditionDz = accessConditionDz,
        accessConditionStatus = accessConditionStatus,
        accessConditionUsage = accessConditionUsage,
        fileReferencesMapping = firstManifestation.fileReferencesMapping(id),
        titlePageId = firstManifestation.titlePageId,
      )

  private def getFirstManifestation(
    root: MetsXml,
    manifestations: List[S3ObjectLocation]): Result[MetsXml] =
    root.firstManifestationFilename
      .flatMap { name =>
        manifestations.find(loc =>
          loc.key.endsWith(name) || loc.key.endsWith(s"$name.xml")) match {
          case Some(location) => Right(location)
          case None =>
            Left(
              new Exception(
                s"Could not find manifestation with filename: $name")
            )
        }
      }
      .flatMap(getMetsXml)

  private def getMetsXml(location: S3ObjectLocation): Result[MetsXml] =
    store
      .get(location)
      .left
      .map(_.e)
      .flatMap { case Identified(_, xmlString) => MetsXml(xmlString) }
}
