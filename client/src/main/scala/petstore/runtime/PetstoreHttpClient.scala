package petstore.runtime

import cats.effect._
import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.show._
import cats.implicits.catsStdShowForLong
import petstore.PetstoreClient
import petstore.models.{Error => PetError, _}
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze._
import org.http4s.circe._
import org.http4s.Status.Successful
import io.circe._
import io.circe.generic.semiauto._
import shapeless.Coproduct

import scala.concurrent.ExecutionContext

object PetstoreHttpClient {
  implicit val newPetDecoder: Encoder[NewPet]       = deriveEncoder[NewPet]
  implicit val updatePetDecoder: Encoder[UpdatePet] = deriveEncoder[UpdatePet]
  implicit val petDecoder: Decoder[Pet]             = deriveDecoder[Pet]
  implicit val petErrorDecoder: Decoder[PetError]   = deriveDecoder[PetError]

  def build[F[_]: Effect](client: Client[F], baseUrl: Uri): PetstoreClient[F] =
    new PetstoreClient[F] {
      import PetstoreClient._
      implicit val newPetEntity: EntityEncoder[F, NewPet]       = jsonEncoderOf[F, NewPet]
      implicit val updatePetEntity: EntityEncoder[F, UpdatePet] = jsonEncoderOf[F, UpdatePet]
      implicit val petEntity: EntityDecoder[F, Pet]             = jsonOf[F, Pet]
      implicit val petErrorEntity: EntityDecoder[F, PetError]   = jsonOf[F, PetError]
      implicit val petsEntity: EntityDecoder[F, List[Pet]]      = jsonOf[F, List[Pet]]

      def createPet(newPet: NewPet): F[Either[CreatePetError, Unit]] =
        client.fetch(
          Request[F](method = Method.POST, uri = baseUrl / "pets").withBody(newPet)
        ) {
          case Successful(response) => response.as[Unit].map(_.asRight)
          case response if response.status == Status.BadRequest =>
            response.as[String].map(x => Coproduct[CreatePetError](DuplicatedPetError(x)).asLeft)
          case default =>
            default.as[PetError].map(x => Coproduct[CreatePetError](UnexpectedError(default.status.code, x)).asLeft)
        }

      def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]] =
        client.expect[List[Pet]](baseUrl / "pets" +?? ("limit", limit) +?? ("name", name))

      def getPet(id: Long): F[Either[GetPetError, Pet]] =
        client.fetch(Request[F](method = Method.GET, uri = baseUrl / "pets" / id.show)) {
          case Successful(response) => response.as[Pet].map(_.asRight)
          case response if response.status == Status.NotFound =>
            response.as[String].map(x => Coproduct[GetPetError](NotFoundError(x)).asLeft)
          case default =>
            default.as[PetError].map(x => Coproduct[GetPetError](UnexpectedError(default.status.code, x)).asLeft)
        }

      def updatePet(id: Long, updatePet: UpdatePet): F[Unit] =
        client.expect[Unit](
          Request[F](method = Method.PUT, uri = baseUrl / "pets" / id.show).withBody(updatePet)
        )

      def deletePet(id: Long): F[Unit] =
        client.expect[Unit](
          Request[F](method = Method.DELETE, uri = baseUrl / "pets" / id.show)
        )
    }

  def apply[F[_]: ConcurrentEffect](baseUrl: Uri)(implicit executionContext: ExecutionContext): F[PetstoreClient[F]] =
    Http1Client[F](config = BlazeClientConfig.defaultConfig.copy(executionContext = executionContext))
      .map(PetstoreHttpClient.build(_, baseUrl))
}
