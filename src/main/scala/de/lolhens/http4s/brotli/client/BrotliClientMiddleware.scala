package de.lolhens.http4s.brotli.client

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import cats.syntax.all._
import de.lhns.fs2.compress.BrotliDecompressor
import de.lolhens.http4s.brotli.BrotliMiddleware
import org.http4s.client.Client
import org.http4s.headers.`Accept-Encoding`
import org.http4s.{ContentCoding, Request}

/** Client middleware for enabling brotli decompression using fs2-compress-brotli.
  */
object BrotliClientMiddleware {

  def apply[F[_]: Async: BrotliDecompressor](client: Client[F]): Client[F] =
    Client[F] { req =>
      val reqWithEncoding  = addHeaders(req)
      val responseResource = client.run(reqWithEncoding)

      responseResource.map { actualResponse =>
        BrotliMiddleware.decompress(actualResponse)
      }
    }

  private def addHeaders[F[_]](req: Request[F]): Request[F] =
    req.headers.get[`Accept-Encoding`] match {
      case Some(h) if h.values.contains_(ContentCoding.br) =>
        req
      case _                                               =>
        req.addHeader(`Accept-Encoding`(NonEmptyList.of(ContentCoding.br)))
    }
}
