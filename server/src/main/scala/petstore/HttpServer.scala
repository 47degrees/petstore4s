/*
 * Copyright 2019 47 Degrees, LLC. <http://www.47deg.com>
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
import fs2.StreamApp.ExitCode
import fs2.{Stream, StreamApp}
import org.http4s.server.blaze.BlazeBuilder
import cats.syntax.flatMap._
import scala.concurrent.ExecutionContext
class HttpServer[F[_]: ConcurrentEffect](petstoreService: F[PetstoreService[F]])(
    implicit executionContext: ExecutionContext
) extends StreamApp[F] {
  def stream(args: List[String], requestShutdown: F[Unit]): Stream[F, ExitCode] = {
    for {
      petstoreEndpoint <- Stream.eval(petstoreService).map(PetstoreEndpoint.apply[F])
      exitCode <- BlazeBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .mountService(petstoreEndpoint, "")
        .serve
    } yield exitCode
  }
}

import ExecutionContext.Implicits.global
object MemoryServerApp extends HttpServer[IO](MemoryPetstoreService())
