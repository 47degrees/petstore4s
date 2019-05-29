package petstore.runtime

import cats.effect._
import cats.syntax.functor._
import cats.syntax.either._
import cats.syntax.flatMap._
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
import io.chrisdavenport.log4cats.Logger

import scala.concurrent.ExecutionContext

object PetstoreHttpClient {
  implicit val newPetDecoder: Encoder[NewPet] = deriveEncoder[NewPet]
  implicit val petDecoder: Decoder[Pet]       = deriveDecoder[Pet]

  def build[F[_]: Effect](client: Client[F], baseUrl: Uri)(implicit L: Logger[F]): PetstoreClient[F] =
    new PetstoreClient[F] {
      implicit val newPetEntity: EntityEncoder[F, NewPet]  = jsonEncoderOf[F, NewPet]
      implicit val petEntity: EntityDecoder[F, Pet]        = jsonOf[F, Pet]
      implicit val petsEntity: EntityDecoder[F, List[Pet]] = jsonOf[F, List[Pet]]

      def createPet(newPet: NewPet): F[Unit] =
        for {
          _ <- client.expect[Unit](
            Request[F](uri = baseUrl / "pets", method = Method.POST).withBody(newPet)
          )
          _ <- L.info(s"Pet $newPet has been created!")
        } yield ()

      def getPets(limit: Option[Int]): F[List[Pet]] =
        for {
          pets <- client.expect[List[Pet]](limit.fold(baseUrl / "pets")(baseUrl / "pets" +? ("limit", _)))
          _    <- L.info(s"Pets with limit($limit): [${pets.mkString(", ")}]")
        } yield pets

      def getPet(id: Long): F[Either[PetError, Pet]] =
        for {
          result <- client.fetch(Request[F](method = Method.GET, uri = baseUrl / "pets" / id.show)) { handleResponse }
          _      <- L.debug(s"Pet by id($id): $result")
        } yield result

      private def handleResponse: Response[F] => F[Either[PetError, Pet]] = {
        case Successful(response) => response.as[Pet].map(_.asRight)
        case default              => default.as[String].map(PetError(default.status.code, _).asLeft)
      }
    }

  def apply[F[_]: ConcurrentEffect: Logger](baseUrl: Uri)(
      implicit executionContext: ExecutionContext): F[PetstoreClient[F]] =
    Http1Client[F](config = BlazeClientConfig.defaultConfig.copy(executionContext = executionContext))
      .map(PetstoreHttpClient.build(_, baseUrl))
}
