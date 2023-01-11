package de.lolhens.http4s.brotli

import cats.effect.kernel.Async
import de.lhns.fs2.compress.BrotliDecompressor
import org.http4s.headers.{`Content-Encoding`, `Content-Length`}
import org.http4s.{ContentCoding, HttpRoutes, Message, Request}

object BrotliMiddleware {
  def decompress[F[_] : Async](message: Message[F],
                               chunkSize: Int = BrotliDecompressor.defaultChunkSize): message.Self =
    message.headers.get[`Content-Encoding`].map(_.contentCoding) match {
      case Some(ContentCoding.br) =>
        message
          .withBodyStream(message.body.through(BrotliDecompressor[F](chunkSize).decompress))
          .removeHeader[`Content-Encoding`]
          .removeHeader[`Content-Length`]

      case _ => message.covary
    }

  def apply[F[_] : Async](routes: HttpRoutes[F],
                          chunkSize: Int = BrotliDecompressor.defaultChunkSize): HttpRoutes[F] =
    routes.local[Request[F]](decompress(_, chunkSize))
}
