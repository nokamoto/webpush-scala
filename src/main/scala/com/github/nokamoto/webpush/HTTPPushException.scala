package com.github.nokamoto.webpush

import java.io.IOException

import com.squareup.okhttp.Request

case class HTTPPushException(request: Request, cause: IOException)
    extends RuntimeException(cause)
