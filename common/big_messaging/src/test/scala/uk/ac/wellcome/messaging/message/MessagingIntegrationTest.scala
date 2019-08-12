package uk.ac.wellcome.messaging.message

import java.util.concurrent.ConcurrentLinkedDeque

import com.amazonaws.services.cloudwatch.model.StandardUnit
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{
  SubscribeRequest,
  SubscribeResult,
  UnsubscribeRequest
}
import io.circe.Encoder
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{Assertion, FunSpec, Matchers}
import uk.ac.wellcome.bigmessaging.BigMessageSender
import uk.ac.wellcome.bigmessaging.message.MessageStream
import uk.ac.wellcome.fixtures.{fixture, Fixture, TestWith}
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.MessageSender
import uk.ac.wellcome.messaging.fixtures.Messaging
import uk.ac.wellcome.messaging.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.fixtures.SQS.Queue
import uk.ac.wellcome.messaging.sns.{SNSConfig, SNSMessageSender}
import uk.ac.wellcome.monitoring.memory.MemoryMetrics
import uk.ac.wellcome.storage.{ObjectStore, StorageBackend}
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.storage.streaming.Codec
import uk.ac.wellcome.storage.streaming.CodecInstances._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MessagingIntegrationTest
    extends FunSpec
    with Matchers
    with Messaging
    with Eventually
    with IntegrationPatience {

  def createMessage(size: Int) = ExampleObject("a" * size)

  val smallMessage: ExampleObject = createMessage(size = 100)
  val largeMessage: ExampleObject = createMessage(size = 300000)

  val subject = "message-integration-test-subject"

  it("sends and receives a message <256KB") {
    assertMessagesCanBeSentAndReceived(List(smallMessage))
  }

  it("sends and receives a message >256KB") {
    assertMessagesCanBeSentAndReceived(List(largeMessage))
  }

  it("sends and receives messages with a mixture of sizes") {
    val sizes = List(10, 50, 100, 280000, 20, 290000)
    assertMessagesCanBeSentAndReceived(
      sizes.map { createMessage }
    )
  }

  private def assertMessagesCanBeSentAndReceived(
    messages: List[ExampleObject]): Assertion =
    withLocalStackBigMessageSenderMessageStream {
      case (messageStream, messageSender) =>
        val receivedMessages = new ConcurrentLinkedDeque[ExampleObject]()

        messages.map { msg =>
          messageSender.sendT(msg)
        }

        messageStream.foreach(
          "integration-test-stream",
          obj => Future { receivedMessages.push(obj) })
        eventually {
          receivedMessages should contain theSameElementsAs messages
        }
    }

  def withLocalStackSubscription[R](queue: Queue,
                                    topic: Topic): Fixture[SubscribeResult, R] =
    fixture[SubscribeResult, R](
      create = {
        val subRequest = new SubscribeRequest(topic.arn, "sqs", queue.arn)
        info(s"Subscribing queue ${queue.arn} to topic ${topic.arn}")

        localStackSnsClient.subscribe(subRequest)
      },
      destroy = { subscribeResult =>
        val unsubscribeRequest =
          new UnsubscribeRequest(subscribeResult.getSubscriptionArn)
        localStackSnsClient.unsubscribe(unsubscribeRequest)
      }
    )

  private def withLocalStackBigMessageSenderMessageStream[R](
    testWith: TestWith[(MessageStream[ExampleObject],
                        BigMessageSender[SNSConfig, ExampleObject]),
                       R]): R = {
    withLocalStackMessageStreamFixtures[R] {
      case (queue, messageStream) =>
        withLocalS3Bucket { bucket =>
          withLocalStackSnsTopic { topic =>
            withLocalStackSubscription(queue, topic) { _ =>
              withBigMessageSender(bucket, topic, localStackSnsClient) {
                messageWriter =>
                  testWith((messageStream, messageWriter))
              }
            }
          }
        }
    }
  }

  def withBigMessageSender[R](bucket: Bucket,
                              topic: Topic,
                              senderSnsClient: AmazonSNS = snsClient)(
    testWith: TestWith[BigMessageSender[SNSConfig, ExampleObject], R])(
    implicit
    circeEncoder: Encoder[ExampleObject],
    objectCodec: Codec[ExampleObject]
  ): R = {
    val sender = new BigMessageSender[SNSConfig, ExampleObject] {
      override val messageSender: MessageSender[SNSConfig] =
        new SNSMessageSender(
          snsClient = senderSnsClient,
          snsConfig = createSNSConfigWith(topic),
          subject = "Sent in MessagingIntegrationTest"
        )
      override val objectStore: ObjectStore[ExampleObject] =
        new ObjectStore[ExampleObject] {
          override implicit val codec: Codec[ExampleObject] = objectCodec
          override implicit val storageBackend: StorageBackend =
            s3StorageBackend
        }
      override val namespace: String = bucket.name
      override implicit val encoder: Encoder[ExampleObject] = circeEncoder
      override val maxMessageSize: Int = 10000
    }

    testWith(sender)
  }

  def withLocalStackMessageStreamFixtures[R](
    testWith: TestWith[(Queue, MessageStream[ExampleObject]), R]): R =
    withActorSystem { implicit actorSystem =>
      val metrics = new MemoryMetrics[StandardUnit]()
      withLocalStackSqsQueue { queue =>
        withMessageStream[ExampleObject, R](queue, metrics) { messageStream =>
          testWith((queue, messageStream))
        }
      }
    }
}
