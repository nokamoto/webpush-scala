package com.github.nokamoto.webpush

import java.util.Base64
import java.util.Date

import com.auth0.jwt.JWT
import com.github.nokamoto.webpush.protobuf.Message
import com.google.protobuf.ByteString
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.squareup.okhttp.HttpUrl
import com.squareup.okhttp.OkHttpClient
import org.scalatest.Matchers._
import org.scalatest.Assertion
import org.scalatest.AsyncFlatSpec

import scala.concurrent.Future
import scala.collection.JavaConverters._

class WebpushClientSpec extends AsyncFlatSpec {
  private[this] def decode(s: String): Array[Byte] = Base64.getDecoder.decode(s)

  private[this] val keyPair = KeyPair(
    privateKey = decode("AJFotoB4FS7IX6tbm5t0SGyISTQ6l54mMzpfYipdOD+N"),
    publicKey = decode(
      "BNuvjW90TpDawYyxhvK79QVyNEplaSQZOWo1CwXDmWwfya6qnyBvIx3tFvKEBetExvil4rNNRL0/ZR2WLjGEAbQ=")
  )

  private[this] def message(endpoint: HttpUrl): Message = {
    val auth = decode("LsUmSxGzGt+KcuczkTfFrQ==")
    val p256dh = decode(
      "BOVFfCoBB/2Sn6YZrKytKc1asM+IOXFKz6+T1NLOnrGrRXh/xJEgiJIoFBO9I6twWDAj6OYvhval8jxq8F4K0iM=")

    Message().update(
      _.subscription.endpoint := endpoint.toString,
      _.subscription.auth := ByteString.copyFrom(auth),
      _.subscription.p256Dh := ByteString.copyFrom(p256dh),
      _.ttl := 1,
      _.plaintext := "test"
    )
  }

  def mockServer(f: MockWebServer => Future[Assertion]): Future[Assertion] = {
    val server = new MockWebServer()
    server.start()

    val future = f(server)
    future.onComplete(_ => server.shutdown())
    future
  }

  "WebpushClient" should "be succeeded if push service returns 201" in {
    val sut = new WebpushClient(client = new OkHttpClient(),
                                keyPair = keyPair,
                                subject = None)

    mockServer { server =>
      val msg = message(server.url("/test"))

      server.enqueue(new MockResponse().setResponseCode(201))

      sut.sendAsync(msg).map { res =>
        assert(res.code() === 201)

        val req = server.takeRequest()
        val vapid = "vapid t=([^,]+),k=(.+)".r

        req.getHeader("Authorization") match {
          case vapid(t, k) =>
            val jwt = JWT.decode(t)
            val now = new Date()
            val twentyFourHoursLater =
              new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)
            assert(jwt.getType === "JWT")
            assert(jwt.getAlgorithm === "ES256")

            assert(jwt.getAudience.asScala === List("http://localhost"))
            assert(jwt.getExpiresAt.after(now))
            assert(jwt.getExpiresAt.before(twentyFourHoursLater))
            assert(jwt.getSubject === null)

            assert(
              k === Base64.getUrlEncoder
                .withoutPadding()
                .encodeToString(keyPair.publicKey))

          case e => fail(s"$vapid does not match $e")
        }

        assert(req.getHeader("TTL") === msg.ttl.toString)
        assert(req.getHeader("Content-Encoding") === "aes128gcm")
        assert(req.getHeader("Content-Length").toLong > 0)
        assert(req.getHeader("Content-Type") === "application/octet-stream")
      }
    }
  }
}
