/*
 * Copyright 2019-2020 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
      _ <-
        BlazeServerBuilder[F]
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
