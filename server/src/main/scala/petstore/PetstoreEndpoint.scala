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

class PetstoreEndpoint[F[_]: ConcurrentEffect](petstoreService: PetstoreService[F]) extends Http4sDsl[F] {
  implicit val petEncoder: Encoder[Pet]                     = deriveEncoder[Pet]
  implicit val newPetEncoder: Decoder[NewPet]               = deriveDecoder[NewPet]
  implicit val updatePetEncoder: Decoder[UpdatePet]         = deriveDecoder[UpdatePet]
  implicit val newPetEntity: EntityDecoder[F, NewPet]       = jsonOf[F, NewPet]
  implicit val updatePetEntity: EntityDecoder[F, UpdatePet] = jsonOf[F, UpdatePet]
  implicit val petEntity: EntityEncoder[F, Pet]             = jsonEncoderOf[F, Pet]
  implicit val petsEntity: EntityEncoder[F, List[Pet]]      = jsonEncoderOf[F, List[Pet]]
  object Limit extends OptionalQueryParamDecoderMatcher[Int]("limit")
  object Name  extends OptionalQueryParamDecoderMatcher[String]("name")

  def service: HttpService[F] =
    HttpService {
      case req @ POST -> Root / "pets" =>
        req.decode[NewPet] { newPet =>
          for {
            _      <- petstoreService.createPet(newPet)
            result <- Created()
          } yield result
        }

      case GET -> Root / "pets" :? Limit(limit) :? Name(name) =>
        petstoreService.getPets(limit, name).flatMap(Ok(_))

      case GET -> Root / "pets" / LongVar(id) =>
        petstoreService.getPet(id).flatMap(_.fold(NotFound(s"Not found pet with id: $id"))(Ok(_)))

      case req @ PUT -> Root / "pets" / LongVar(id) =>
        req.decode[UpdatePet] { newPet =>
          for {
            _      <- petstoreService.updatePet(id, newPet)
            result <- Ok()
          } yield result
        }
      case req @ DELETE -> Root / "pets" / LongVar(id) =>
        petstoreService.deletePet(id).flatMap(Ok(_))
    }
}

object PetstoreEndpoint {
  def apply[F[_]: ConcurrentEffect](petstoreService: PetstoreService[F]): HttpService[F] =
    new PetstoreEndpoint[F](petstoreService).service
}
