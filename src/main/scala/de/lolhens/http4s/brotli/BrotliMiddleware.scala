package de.lolhens.http4s.brotli

import cats.effect.kernel.Async
import fs2.Pipe
import org.brotli.dec.BrotliInputStream
import org.http4s.headers.{`Content-Encoding`, `Content-Length`}
import org.http4s.{ContentCoding, HttpRoutes, Message, Request}

object BrotliMiddleware {
  def brotliDecompress[F[_] : Async](bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): Pipe[F, Byte, Byte] =
    _.through(fs2.io.toInputStream)
      .flatMap { inputStream =>
        fs2.io.readInputStream(Async[F].blocking {
          new BrotliInputStream(inputStream, bufferSize)
        }, bufferSize)
      }

  def decompress[F[_] : Async](message: Message[F],
                               bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): message.Self =
    message.headers.get(`Content-Encoding`).map(_.contentCoding) match {
      case Some(ContentCoding.br) =>
        message
          .withBodyStream(message.body.through(brotliDecompress(bufferSize)))
          .removeHeader(`Content-Encoding`)
          .removeHeader(`Content-Length`)

      case _ => message.covary
    }

  def apply[F[_] : Async](routes: HttpRoutes[F],
                          bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): HttpRoutes[F] =
    routes.local[Request[F]](decompress(_, bufferSize))

}
