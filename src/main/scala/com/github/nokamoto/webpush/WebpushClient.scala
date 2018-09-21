package com.github.nokamoto.webpush

import webpush.protobuf.Message

import scala.concurrent.Future

class WebpushClient {
  def send(request: Message): Future[Unit] = Future.failed(new RuntimeException)
}
