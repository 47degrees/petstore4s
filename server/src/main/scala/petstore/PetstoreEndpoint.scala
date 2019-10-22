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

import org.http4s._
import org.http4s.circe._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.dsl.Http4sDsl
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._
import models._
import petstore.AnotherPetstoreClient.{CreatePetDuplicatedResponseError, GetPetNotFoundResponseError}

class PetstoreEndpoint[F[_]: ConcurrentEffect](petstoreService: PetstoreService[F]) extends Http4sDsl[F] {
  implicit val petEncoder: Encoder[Pet]                     = deriveEncoder[Pet]
  implicit val petErrorEncoder: Encoder[PetError]           = deriveEncoder[PetError]
  implicit val newPetEncoder: Decoder[NewPet]               = deriveDecoder[NewPet]
  implicit val updatePetEncoder: Decoder[UpdatePet]         = deriveDecoder[UpdatePet]
  implicit val newPetEntity: EntityDecoder[F, NewPet]       = jsonOf[F, NewPet]
  implicit val updatePetEntity: EntityDecoder[F, UpdatePet] = jsonOf[F, UpdatePet]
  implicit val petEntity: EntityEncoder[F, Pet]             = jsonEncoderOf[F, Pet]
  implicit val petsEntity: EntityEncoder[F, List[Pet]]      = jsonEncoderOf[F, List[Pet]]
  implicit val petErrorEntity: EntityEncoder[F, PetError]   = jsonEncoderOf[F, PetError]
  object Limit extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object Name  extends OptionalQueryParamDecoderMatcher[String]("name")

  import shapeless.Poly1

  object GetPetResponseHandler extends Poly1 {
    implicit val peh1 = at[GetPetNotFoundResponseError](e => NotFound(e.value))
    implicit val peh2 = at[PetError](e => InternalServerError(e))
  }

  object CreatePetResponseHandler extends Poly1 {
    implicit val peh1 = at[CreatePetDuplicatedResponseError](e => BadRequest(e.value))
    implicit val peh2 = at[PetError](e => InternalServerError(e))
  }

  def service: HttpService[F] =
    HttpService {
      case req @ POST -> Root / "pets" =>
        req.decode[NewPet] { newPet =>
          petstoreService
            .createPet(newPet)
            .flatMap(
              _.fold(_.map(CreatePetResponseHandler).unify, _ => Created())
            )
        }

      case GET -> Root / "pets" :? Limit(limit) :? Name(name) =>
        petstoreService.getPets(limit, name).flatMap(Ok(_))

      case GET -> Root / "pets" / LongVar(id) =>
        petstoreService
          .getPet(id)
          .flatMap(_.fold(_.map(GetPetResponseHandler).unify, Ok(_)))

      case req @ PUT -> Root / "pets" / LongVar(id) =>
        req.decode[UpdatePet] { newPet =>
          petstoreService.updatePet(id, newPet).flatMap(_ => Ok())
        }
      case req @ DELETE -> Root / "pets" / LongVar(id) =>
        petstoreService.deletePet(id).flatMap(Ok(_))
    }
}

object PetstoreEndpoint {
  def apply[F[_]: ConcurrentEffect](petstoreService: PetstoreService[F]): HttpService[F] =
    new PetstoreEndpoint[F](petstoreService).service
}
