package de.lolhens.http4s.brotli

import cats.effect.{Blocker, ConcurrentEffect, ContextShift}
import fs2.Pipe
import org.brotli.dec.BrotliInputStream
import org.http4s.headers.{`Content-Encoding`, `Content-Length`}
import org.http4s.{ContentCoding, HttpRoutes, Response}

object BrotliMiddleware {
  def brotliDecompress[F[_] : ConcurrentEffect : ContextShift](blocker: Blocker,
                                                               bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): Pipe[F, Byte, Byte] =
    _.through(fs2.io.toInputStream)
      .flatMap { inputStream =>
        fs2.io.readInputStream(blocker.delay {
          new BrotliInputStream(inputStream, bufferSize)
        }, bufferSize, blocker)
      }

  def response[F[_] : ConcurrentEffect : ContextShift](response: Response[F],
                                                       blocker: Blocker,
                                                       bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): Response[F] =
    response.headers.get(`Content-Encoding`).map(_.contentCoding) match {
      case Some(ContentCoding.br) =>
        response
          .withBodyStream(response.body.through(brotliDecompress(blocker, bufferSize)))
          .removeHeader(`Content-Encoding`)
          .removeHeader(`Content-Length`)

      case _ => response
    }

  def apply[F[_] : ConcurrentEffect : ContextShift](routes: HttpRoutes[F],
                                                    blocker: Blocker,
                                                    bufferSize: Int = BrotliInputStream.DEFAULT_INTERNAL_BUFFER_SIZE): HttpRoutes[F] =
    routes.map(response(_, blocker, bufferSize))

}
