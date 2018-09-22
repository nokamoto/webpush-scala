package com.github.nokamoto.webpush

import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.Base64
import java.util.Date

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.crypto.tink.apps.webpush.WebPushHybridEncrypt
import com.google.crypto.tink.subtle.EllipticCurves
import com.squareup.okhttp._
import webpush.protobuf.Message

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.control.NonFatal

/**
  * An implementation of [[https://tools.ietf.org/html/rfc8030]], [[https://tools.ietf.org/html/rfc8291]], and [[https://tools.ietf.org/html/rfc8292]].
  *
  * @param subject application server contact information described in [[https://tools.ietf.org/html/rfc8292#section-2.1]].
  */
class WebpushClient(client: OkHttpClient,
                    keyPair: KeyPair,
                    subject: Option[String]) {
  private[this] def encrypt(message: Message): Array[Byte] = {
    try {
      val aes128gcm = new WebPushHybridEncrypt.Builder()
        .withAuthSecret(message.getSubscription.auth.toByteArray)
        .withRecipientPublicKey(message.getSubscription.p256Dh.toByteArray)
        .build()

      val plaintext = message.plaintext.getBytes(Charset.defaultCharset())

      aes128gcm.encrypt(plaintext, null) // contextInfo, must be null
    } catch {
      case NonFatal(cause) => throw new MessageEncryptionException(cause)
    }
  }

  private[this] val k =
    Base64.getUrlEncoder.withoutPadding().encodeToString(keyPair.publicKey)

  private[this] val alg = Algorithm.ECDSA256(
    null,
    EllipticCurves.getEcPrivateKey(EllipticCurves.CurveType.NIST_P256,
                                   keyPair.privateKey))

  private[this] val mediaType = MediaType.parse("application/octet-stream")

  private[this] def t(message: Message): String = {
    try {
      val url = new URL(message.getSubscription.endpoint)
      val aud = s"${url.getProtocol}://${url.getHost}" // not sure if endpoint contains port like https://updates.push.services.mozilla.com:443/...

      val exp = new Date(System.currentTimeMillis() + 12 * 60 * 60 * 1000)

      val jwt = JWT.create().withAudience(aud).withExpiresAt(exp)

      subject.foldLeft(jwt) { case (t, sub) => t.withSubject(sub) }.sign(alg)
    } catch {
      case NonFatal(cause) => throw new JWTException(cause)
    }
  }

  /**
    * @throws MessageEncryptionException if aes128gcm encoding failed.
    * @throws JWTException if JSON Web Token signing failed.
    */
  def send(message: Message): Call = {
    val ciphertext = encrypt(message)
    val t_ = t(message)
    val body = RequestBody.create(mediaType, ciphertext)

    val req = new Request.Builder()
      .url(message.getSubscription.endpoint)
      .post(body)
      .addHeader("Content-Encoding", "aes128gcm")
      .addHeader("TTL", message.ttl.toString)
      .addHeader("Authorization", s"vapid t=$t_,k=$k")
      .build()

    client.newCall(req)
  }

  /**
    * @return [[HTTPPushException]] if [[IOException]] raised.
    *         [[MessageEncryptionException]] if aes128gcm encoding failed.
    *         [[JWTException]] if JSON Web Token signing failed.
    */
  def sendAsync(message: Message): Future[Response] = {
    val p = Promise[Response]

    try {
      send(message).enqueue(new Callback {
        override def onFailure(request: Request, e: IOException): Unit =
          p.failure(HTTPPushException(request, e))

        override def onResponse(response: Response): Unit = p.success(response)
      })
    } catch {
      case e: MessageEncryptionException => p.failure(e)
      case e: JWTException               => p.failure(e)
    }

    p.future
  }
}
