package petstore

import cats.effect._
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import cats.syntax.flatMap._
import scala.concurrent.ExecutionContext
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
class HttpServer[F[_]: Effect](petstoreService: F[PetstoreService[F]])(implicit executionContext: ExecutionContext)
    extends StreamApp[F] {
  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    Stream
      .eval(
        Slf4jLogger
          .fromName[F]("PetstoreServer"))
      .flatMap { implicit log =>
        for {
          petstoreEndpoint <- Stream.eval(petstoreService).map(PetstoreEndpoint.apply[F])
          exitCode <- BlazeBuilder[F]
            .bindHttp(8080, "0.0.0.0")
            .mountService(petstoreEndpoint, "")
            .serve
        } yield exitCode
      }
  }

}

import ExecutionContext.Implicits.global
object MemoryServerApp extends HttpServer[IO](MemoryPetstoreService())
