package com.github.nokamoto.webpush

import java.util.Base64

import com.github.nokamoto.webpush.WebpushTestingServiceSpec.FirefoxTest
import com.github.nokamoto.webpush.WebpushTestingServiceSpec.Suite
import com.github.nokamoto.webpush.protobuf.Message
import com.github.nokamoto.webpush.protobuf.PushSubscription
import com.google.protobuf.ByteString
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.scalatest.FlatSpec
import org.scalatest.Tag
import play.api.libs.json.Json
import play.api.libs.json.OFormat

/**
  * @see [[https://github.com/nokamoto/webpush-testing-service]]
  */
class WebpushTestingServiceSpec extends FlatSpec {
  private[this] def decode(s: String): Array[Byte] = Base64.getDecoder.decode(s)

  private[this] val keyPair = KeyPair(
    privateKey = decode("AJFotoB4FS7IX6tbm5t0SGyISTQ6l54mMzpfYipdOD+N"),
    publicKey = decode(
      "BNuvjW90TpDawYyxhvK79QVyNEplaSQZOWo1CwXDmWwfya6qnyBvIx3tFvKEBetExvil4rNNRL0/ZR2WLjGEAbQ=")
  )

  private[this] val testingUrl = "http://localhost:9000/testing"

  "WebpushClient" should "send a message to the firefox push service" taggedAs FirefoxTest in {
    val okHttpClient = new OkHttpClient()
    val empty = RequestBody.create(null, "")

    val startSuite = okHttpClient
      .newCall(new Request.Builder().url(testingUrl).post(empty).build())
      .execute()
    assert(startSuite.code() === 201)

    val suite = Json.parse(startSuite.body().string()).as[Suite]

    try {
      assert(suite.events === Nil)

      val client = new WebpushClient(okHttpClient,
                                     keyPair,
                                     Some("mailto:nokamoto.engr@gmail.com"))
      val plaintext =
        Json.obj("id" -> suite.id, "message" -> "hello world").toString()
      val message =
        Message().update(_.subscription := suite.subscription.asProto,
                         _.ttl := 30,
                         _.plaintext := plaintext)

      val res = client.send(message).execute()
      assert(res.code() === 201)

      Thread.sleep(10 * 1000) // Wait for transmission of the message, push service -> service worker -> webpush testing service

      val getEvent = okHttpClient
        .newCall(
          new Request.Builder().url(s"$testingUrl/${suite.id}").get().build())
        .execute()

      val events = Json.parse(getEvent.body().string()).as[Suite]
      assert(events.events === plaintext :: Nil)
    } finally {
      okHttpClient
        .newCall(
          new Request.Builder()
            .url(s"$testingUrl/${suite.id}")
            .delete()
            .build())
        .execute()
    }
  }
}

object WebpushTestingServiceSpec {
  object FirefoxTest extends Tag("com.github.nokamoto.webpush.FirefoxTest")

  case class Subscription(endpoint: String, auth: String, p256dh: String) {
    private[this] def decode(s: String): Array[Byte] =
      Base64.getDecoder.decode(s)

    def asProto: PushSubscription = PushSubscription().update(
      _.endpoint := endpoint,
      _.auth := ByteString.copyFrom(decode(auth)),
      _.p256Dh := ByteString.copyFrom(decode(p256dh))
    )
  }

  case class Suite(id: String, subscription: Subscription, events: List[String])

  implicit val subscription: OFormat[Subscription] = Json.format[Subscription]
  implicit val suite: OFormat[Suite] = Json.format[Suite]
}
