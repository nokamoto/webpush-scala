package com.github.nokamoto.webpush

/**
  * An application server key pair described in [[https://tools.ietf.org/html/rfc8292#section-2]].
  */
case class KeyPair(privateKey: Array[Byte], publicKey: Array[Byte])
