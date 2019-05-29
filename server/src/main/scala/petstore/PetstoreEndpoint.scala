package petstore

import org.http4s._
import org.http4s.circe._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.dsl.Http4sDsl
import cats.effect._
import cats.syntax.flatMap._
import cats.syntax.functor._

import io.chrisdavenport.log4cats.Logger

import models._

class PetstoreEndpoint[F[_]: ConcurrentEffect](petstoreService: PetstoreService[F])(implicit L: Logger[F])
    extends Http4sDsl[F] {
  implicit val petEncoder: Encoder[Pet]                = deriveEncoder[Pet]
  implicit val newPetEncoder: Decoder[NewPet]          = deriveDecoder[NewPet]
  implicit val newPetEntity: EntityDecoder[F, NewPet]  = jsonOf[F, NewPet]
  implicit val petEntity: EntityEncoder[F, Pet]        = jsonEncoderOf[F, Pet]
  implicit val petsEntity: EntityEncoder[F, List[Pet]] = jsonEncoderOf[F, List[Pet]]
  object Limit extends OptionalQueryParamDecoderMatcher[Int]("limit")

  def service: HttpService[F] =
    HttpService {
      case req @ POST -> Root / "pets" =>
        req.decode[NewPet] { newPet =>
          for {
            _      <- petstoreService.createPet(newPet)
            _      <- L.debug(s"$newPet has been created!")
            result <- Created()
          } yield result
        }
      case GET -> Root / "pets" :? Limit(limit) =>
        petstoreService.getPets(limit).flatMap(Ok(_))

      case GET -> Root / "pets" / LongVar(id) =>
        petstoreService.getPet(id).flatMap(_.fold(NotFound(s"Not found pet with id: $id"))(Ok(_)))
    }
}

object PetstoreEndpoint {
  def apply[F[_]: ConcurrentEffect: Logger](petstoreService: PetstoreService[F]): HttpService[F] =
    new PetstoreEndpoint[F](petstoreService).service
}
