package petstore
package client

import cats.effect._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s._
import org.http4s.client.blaze.BlazeClientBuilder
import petstore.models.NewPet

import scala.concurrent.ExecutionContext.Implicits.global

object Boot extends IOApp {
  lazy val L: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def run(args: List[String]): IO[ExitCode] = {

    BlazeClientBuilder[IO](global).resource.use { client =>
      val petStoreClient: AnotherPetstoreClient[IO] = AnotherPetstoreHttpClient.build[IO](
        client,
        uri"http://localhost:8080/"
      )

      (for {
        _         <- petStoreClient.createPet(NewPet("Beethoven", None))
        beethoven <- petStoreClient.getPet(petId = 1)
        _         <- L.info(s"Pet = $beethoven")
      } yield ExitCode.Success)

//      def getPets(limit: Option[Int], name: Option[String]): F[Pets]
//      def createPet(newPet: NewPet): F[Either[CreatePetErrorResponse, Unit]]
//      def getPet(petId: Int): F[Either[GetPetErrorResponse, Pet]]
//      def deletePet(petId: Int): F[Unit]
//      def updatePet(petId: Int, updatePet: UpdatePet): F[Unit]

    }

  }
}
