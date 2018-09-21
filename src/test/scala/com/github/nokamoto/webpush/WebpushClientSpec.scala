package com.github.nokamoto.webpush

import org.scalatest.AsyncFlatSpec
import webpush.protobuf.Message

class WebpushClientSpec extends AsyncFlatSpec {
  "WebpushClient#send" should "be not implemented yet" in {
    val sut = new WebpushClient

    sut.send(Message()).map(_ => fail()).recover {
      case _: RuntimeException => pending
    }
  }
}
