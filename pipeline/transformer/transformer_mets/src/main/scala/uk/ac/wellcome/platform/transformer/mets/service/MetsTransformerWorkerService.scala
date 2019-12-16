package uk.ac.wellcome.platform.transformer.mets.service

import scala.concurrent.Future
import akka.Done
import grizzled.slf4j.Logging

import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.sns.{NotificationMessage, SNSConfig}
import uk.ac.wellcome.messaging.sqs.SQSStream
import uk.ac.wellcome.mets_adapter.models.MetsLocation
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.mets.transformer.MetsXml
import uk.ac.wellcome.storage.store.{VersionedStore, Readable}
import uk.ac.wellcome.storage.{Identified, Version, ObjectLocation}
import uk.ac.wellcome.typesafe.Runnable

class MetsTransformerWorkerService(
  msgStream: SQSStream[NotificationMessage],
  messageSender: BigMessageSender[SNSConfig, TransformedBaseWork],
  adapterStore: VersionedStore[String, Int, MetsLocation],
  metsXmlStore: Readable[ObjectLocation, String]
) extends Runnable
    with Logging {

  type Result[T] = Either[Throwable, T]

  val className = this.getClass.getSimpleName

  def run(): Future[Done] =
    msgStream.foreach(this.getClass.getSimpleName, processAndLog)

  def processAndLog(message: NotificationMessage): Future[Unit] = {
    val tried = for {
      key <- fromJson[Version[String, Int]](message.body)
      _ <- process(key).toTry
    } yield ()
    Future.fromTry(tried.recover {
      case t =>
        error(s"There was an error processing $message: ", t)
        throw t
    })
  }

  private def process(key: Version[String, Int]) =
    for {
      metsLocation <- getMetsLocation(key)
      metsXml <- getMetsXml(metsLocation)
      mets <- metsXml.toMetsData
      work <- mets.toWork(key.version)
      _ <- messageSender.sendT(work).toEither
    } yield ()

  private def getMetsLocation(key: Version[String, Int]): Result[MetsLocation] =
    adapterStore.get(key) match {
      case Left(err)                   => Left(err.e)
      case Right(Identified(_, entry)) => Right(entry)
    }

  private def getMetsXml(metsLocation: MetsLocation): Result[MetsXml] =
    metsXmlStore
      .get(metsLocation.xmlLocation)
      .left
      .map(_.e)
      .flatMap { case Identified(_, xmlString) => MetsXml(xmlString) }
}
