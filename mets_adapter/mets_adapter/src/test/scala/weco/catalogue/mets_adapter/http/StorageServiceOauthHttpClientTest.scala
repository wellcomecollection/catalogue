package weco.catalogue.mets_adapter.http

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, OAuth2BearerToken}
import akka.http.scaladsl.model._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.akka.fixtures.Akka
import weco.http.client.{HttpGet, HttpPost, MemoryHttpClient}
import weco.http.fixtures.HttpFixtures

import java.net.URL
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class StorageServiceOauthHttpClientTest extends AnyFunSpec with Matchers with Akka with HttpFixtures {
  val credentials: BasicHttpCredentials = BasicHttpCredentials("username", "password")

  val tokenRequest =
    HttpRequest(
      method = HttpMethods.POST,
      headers = List(Authorization(credentials)),
      uri = Uri("http://storage:1234/token"),
      entity = HttpEntity(
        contentType = ContentTypes.`application/x-www-form-urlencoded`,
        "grant_type=client_credentials"
      )
    )

  val bagJson: String =
    """
      |{
      |  "@context": "https://api.wellcomecollection.org/context.json",
      |  "id": "digitised/b16237456",
      |  "space": {
      |    "id": "digitised",
      |    "type": "Space"
      |  },
      |  "info": {
      |    "externalIdentifier": "b16237456",
      |    "payloadOxum": "1138856615.297",
      |    "baggingDate": "2021-05-19",
      |    "sourceOrganization": "intranda GmbH",
      |    "externalDescription": "[The Caldecott Community]",
      |    "internalSenderIdentifier": "4656",
      |    "internalSenderDescription": "sa_eug_d_51_box_35_b16237456",
      |    "type": "BagInfo"
      |  },
      |  "manifest": {
      |    "checksumAlgorithm": "SHA-256",
      |    "files": [
      |      {
      |        "checksum": "00c28fa37208820ff9e621092fe522ef49b7dfe38f027a370b02666efff017a1",
      |        "name": "data/b16237456.xml",
      |        "path": "v3/data/b16237456.xml",
      |        "size": 758093,
      |        "type": "File"
      |      },
      |      {
      |        "checksum": "30266128984e9e702aebc4e2ca89c56ac189818f4e328d1e7bb8570e14acdbc7",
      |        "name": "data/objects/b16237456_0001.JP2",
      |        "path": "v3/data/objects/b16237456_0001.JP2",
      |        "size": 3931320,
      |        "type": "File"
      |      },
      |      {
      |        "checksum": "21b8887ad02ac0e28fb97105e895a0164c3cbf0db1d5dff5747eab2e30401c58",
      |        "name": "data/objects/b16237456_0002.JP2",
      |        "path": "v3/data/objects/b16237456_0002.JP2",
      |        "size": 3679235,
      |        "type": "File"
      |      }
      |    ],
      |    "type": "BagManifest"
      |  },
      |  "tagManifest": {
      |    "checksumAlgorithm": "SHA-256",
      |    "files": [
      |      {
      |        "checksum": "f0164324e54044613b756979343b4fd5a5a5b1ab222d26fca7b4f24679e2b19a",
      |        "name": "bag-info.txt",
      |        "path": "v3/bag-info.txt",
      |        "size": 295,
      |        "type": "File"
      |      }
      |    ],
      |    "type": "BagManifest"
      |  },
      |  "location": {
      |    "provider": {
      |      "id": "amazon-s3",
      |      "type": "Provider"
      |    },
      |    "bucket": "wellcomecollection-storage",
      |    "path": "digitised/b16237456",
      |    "type": "Location"
      |  },
      |  "replicaLocations": [
      |    {
      |      "provider": {
      |        "id": "amazon-s3",
      |        "type": "Provider"
      |      },
      |      "bucket": "wellcomecollection-storage-replica-ireland",
      |      "path": "digitised/b16237456",
      |      "type": "Location"
      |    }
      |  ],
      |  "createdDate": "2021-05-19T12:32:38.589051Z",
      |  "version": "v3",
      |  "type": "Bag"
      |}
      |""".stripMargin

  it("fetches a token before making a request") {
    val token = OAuth2BearerToken("dummy_access_token")

    val responses = Seq(
      (
        tokenRequest,
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            """
              |{
              |  "access_token": "dummy_access_token",
              |  "token_type": "bearer",
              |  "expires_in": 3600
              |}
              |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token)),
          uri = Uri("http://storage:1234/bags/digitised/b16237456")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            bagJson
          )
        )
      )
    )

    val underlying = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://storage:1234")
    }

    withActorSystem { implicit actorSystem =>
      val authClient = new StorageServiceOauthHttpClient(
        underlying,
        credentials = credentials,
        tokenUri = Uri("http://storage:1234/token")
      )

      val future = authClient.get(path = Path("bags/digitised/b16237456"))

      whenReady(future) { resp =>
        withStringEntity(resp.entity) {
          assertJsonStringsAreEqual(_, bagJson)
        }
      }
    }
  }

  it("fetches a new token when the old token expires") {
    val token1 = OAuth2BearerToken("dummy_access_token1")
    val token2 = OAuth2BearerToken("dummy_access_token2")

    val responses = Seq(
      (
        tokenRequest,
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
               |{
               |  "access_token": "${token1.token}",
               |  "token_type": "bearer",
               |  "expires_in": 3
               |}
               |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token1)),
          uri = Uri("http://storage:1234/bags/digitised/b16237456")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            bagJson
          )
        )
      ),
      (
        tokenRequest,
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""
               |{
               |  "access_token": "${token2.token}",
               |  "token_type": "bearer",
               |  "expires_in": 3
               |}
               |""".stripMargin
          )
        )
      ),
      (
        HttpRequest(
          headers = List(Authorization(token2)),
          uri = Uri("http://storage:1234/bags/digitised/b16237456")
        ),
        HttpResponse(
          entity = HttpEntity(
            contentType = ContentTypes.`application/json`,
            bagJson
          )
        )
      )
    )

    val underlying = new MemoryHttpClient(responses) with HttpGet with HttpPost {
      override val baseUri: Uri = Uri("http://storage:1234")
    }

    withActorSystem { implicit actorSystem =>
      val authClient = new StorageServiceOauthHttpClient(
        underlying,
        tokenUri = Uri("http://storage:1234/token"),
        credentials = credentials,
        expiryGracePeriod = 3.seconds
      )

      val future1 = authClient.get(path = Path("bags/digitised/b16237456"))

      whenReady(future1) { resp1 =>
        withStringEntity(resp1.entity) {
          assertJsonStringsAreEqual(_, bagJson)
        }
      }

      Thread.sleep(1000)

      val future2 = authClient.get(path = Path("bags/digitised/b16237456"))

      whenReady(future2) { resp2 =>
        withStringEntity(resp2.entity) {
          assertJsonStringsAreEqual(_, bagJson)
        }
      }
    }
  }

  override def contextUrl: URL = new URL("http://example.net/context.json")
}
