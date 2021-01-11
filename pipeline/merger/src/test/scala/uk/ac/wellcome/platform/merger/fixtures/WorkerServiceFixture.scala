package uk.ac.wellcome.platform.merger.fixtures

import uk.ac.wellcome.fixtures.TestWith
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.memory.MemoryMessageSender
import uk.ac.wellcome.models.work.internal.WorkState.{Identified, Merged}
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.monitoring.Metrics
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.pipeline_storage.fixtures.PipelineStorageStreamFixtures
import uk.ac.wellcome.pipeline_storage.{MemoryIndexer, MemoryRetriever}
import uk.ac.wellcome.platform.merger.services._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WorkerServiceFixture extends PipelineStorageStreamFixtures {
  def withWorkerService[R](retriever: MemoryRetriever[Work[Identified]],
                           queue: Queue,
                           workSender: MemoryMessageSender,
                           imageSender: MemoryMessageSender =
                             new MemoryMessageSender(),
                           metrics: Metrics[Future] = new MemoryMetrics,
                           index: mutable.Map[String, Work[Merged]] =
                             mutable.Map[String, Work[Merged]]())(
    testWith: TestWith[MergerWorkerService[String, String], R]): R =
    withPipelineStream(
      queue = queue,
      indexer = new MemoryIndexer(index),
      sender = workSender,
      metrics = metrics
    ) { pipelineStream =>
      val workerService = new MergerWorkerService(
        pipelineStorageStream = pipelineStream,
        sourceWorkLookup = new IdentifiedWorkLookup(retriever),
        mergerManager = new MergerManager(PlatformMerger),
        imageSender = imageSender
      )

      workerService.run()

      testWith(workerService)
    }

  def withWorkerService[R](retriever: MemoryRetriever[Work[Identified]])(
    testWith: TestWith[MergerWorkerService[String, String], R]): R =
    withLocalSqsQueue() { queue =>
      val workSender = new MemoryMessageSender()
      val imageSender = new MemoryMessageSender()

      withWorkerService(retriever, queue, workSender, imageSender) {
        workerService =>
          testWith(workerService)
      }
    }

  def getWorksSent(workSender: MemoryMessageSender): Seq[String] =
    workSender.messages.map { _.body }

  def getImagesSent(
    imageSender: MemoryMessageSender): Seq[Image[ImageState.Initial]] =
    imageSender.getMessages[Image[ImageState.Initial]]
}
