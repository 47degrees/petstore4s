package petstore

import cats.effect._
import org.http4s.server.blaze.BlazeServerBuilder
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.server.Router
import cats.effect.IO
import org.http4s.implicits._
object HttpServer {
  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](petstoreService: F[PetstoreService[F]]): F[ExitCode] =
    for {
      petstoreEndpoint <- petstoreService.map(PetstoreEndpoint.apply[F])
      _ <- BlazeServerBuilder[F]
        .bindLocal(8080)
        .withHttpApp(Router(("/", petstoreEndpoint)).orNotFound)
        .serve
        .compile
        .drain
    } yield ExitCode.Success
}

object MemoryServerApp extends IOApp {
  def run(args: List[String]): IO[ExitCode] = HttpServer(MemoryPetstoreService[IO]())
}
