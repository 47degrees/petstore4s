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

import scala.concurrent.ExecutionContext

object PetstoreHttpClient {
  implicit val newPetDecoder: Encoder[NewPet]       = deriveEncoder[NewPet]
  implicit val updatePetDecoder: Encoder[UpdatePet] = deriveEncoder[UpdatePet]
  implicit val petDecoder: Decoder[Pet]             = deriveDecoder[Pet]

  def build[F[_]: Effect](client: Client[F], baseUrl: Uri): PetstoreClient[F] =
    new PetstoreClient[F] {
      implicit val newPetEntity: EntityEncoder[F, NewPet]       = jsonEncoderOf[F, NewPet]
      implicit val updatePetEntity: EntityEncoder[F, UpdatePet] = jsonEncoderOf[F, UpdatePet]
      implicit val petEntity: EntityDecoder[F, Pet]             = jsonOf[F, Pet]
      implicit val petsEntity: EntityDecoder[F, List[Pet]]      = jsonOf[F, List[Pet]]

      def createPet(newPet: NewPet): F[Unit] =
        client.expect[Unit](
          Request[F](method = Method.POST, uri = baseUrl / "pets").withBody(newPet)
        )

      def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]] =
        client.expect[List[Pet]](baseUrl / "pets" +?? ("limit", limit) +?? ("name", name))

      def getPet(id: Long): F[Either[PetError, Pet]] =
        client.fetch(Request[F](method = Method.GET, uri = baseUrl / "pets" / id.show)) { handleResponse }

      def updatePet(id: Long, updatePet: UpdatePet): F[Unit] =
        client.expect[Unit](
          Request[F](method = Method.PUT, uri = baseUrl / "pets" / id.show).withBody(updatePet)
        )

      def deletePet(id: Long): F[Unit] =
        client.expect[Unit](
          Request[F](method = Method.DELETE, uri = baseUrl / "pets" / id.show)
        )

      private def handleResponse: Response[F] => F[Either[PetError, Pet]] = {
        case Successful(response) => response.as[Pet].map(_.asRight)
        case default              => default.as[String].map(PetError(default.status.code, _).asLeft)
      }
    }

  def apply[F[_]: ConcurrentEffect](baseUrl: Uri)(implicit executionContext: ExecutionContext): F[PetstoreClient[F]] =
    Http1Client[F](config = BlazeClientConfig.defaultConfig.copy(executionContext = executionContext))
      .map(PetstoreHttpClient.build(_, baseUrl))
}
