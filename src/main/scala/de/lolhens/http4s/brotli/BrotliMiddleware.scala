package de.lolhens.http4s.brotli

import cats.ApplicativeThrow
import cats.effect.kernel.Async
import de.lhns.fs2.compress.BrotliDecompressor
import fs2.{Pipe, Pull, Stream}
import org.http4s.headers.{`Content-Encoding`, `Content-Length`}
import org.http4s.{ContentCoding, HttpRoutes, Message, Request}

import scala.util.control.NoStackTrace

object BrotliMiddleware {
  def decompress[F[_] : Async: BrotliDecompressor](message: Message[F]): message.Self =
    message.headers.get[`Content-Encoding`].map(_.contentCoding) match {
      case Some(ContentCoding.br) =>
        message
          .withBodyStream(message.body.through(decompressWith(BrotliDecompressor[F].decompress)))
          .removeHeader[`Content-Encoding`]
          .removeHeader[`Content-Length`]

      case _ => message.covary
    }

  private def decompressWith[F[_] : ApplicativeThrow](decompressor: Pipe[F, Byte, Byte]): Pipe[F, Byte, Byte] =
    _.pull.peek1
      .flatMap {
        case None => Pull.raiseError(EmptyBodyException)
        case Some((_, fullStream)) => Pull.output1(fullStream)
      }
      .stream
      .flatten
      .through(decompressor)
      .handleErrorWith {
        case EmptyBodyException => Stream.empty
        case error => Stream.raiseError(error)
      }

  private object EmptyBodyException extends Throwable with NoStackTrace

  def apply[F[_] : Async: BrotliDecompressor](routes: HttpRoutes[F]): HttpRoutes[F] =
    routes.local[Request[F]](decompress)
}
