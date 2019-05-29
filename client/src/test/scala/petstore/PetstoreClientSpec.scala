package petstore

import org.scalatest._
import Matchers._
import org.scalactic.TypeCheckedTripleEquals
import cats.implicits._
import models._

class PetstoreClientSpec extends FlatSpec with TypeCheckedTripleEquals with EitherValues {
  import PetstoreClientSpec._

  "Petstore client" should "get the pets" in {
    val expectedPets = List(Pet(1, "a"), Pet(2, "b"), Pet(3, "c"))
    withClient(expectedPets) { _.getPets(none).map(_ should ===(expectedPets)) }
  }

  it should "get the pets when the limit is established" in {
    val expectedPets = List(Pet(1, "a"), Pet(2, "b"))
    withClient(expectedPets) { _.getPets(1.some).map(_ should ===(expectedPets.take(1))) }
  }

  it should "get the pets by id" in {
    val expectedPet = Pet(1, "a")
    withClient(List(expectedPet, Pet(2, "b"))) { _.getPet(1).map(_.right.value should ===(expectedPet)) }
  }

  it should "not get the pets by id when the pet does not exist" in {
    withClient(List(Pet(1, "a"), Pet(2, "b"))) {
      _.getPet(3).map(_.left.value should ===(Error(404, "Not found pet with id: 3")))
    }
  }

  it should "able to create new pets" in {
    withClient() { client =>
      for {
        _    <- client.createPet(NewPet("name"))
        pets <- client.getPets(none)
      } yield pets should ===(List(Pet(1, "name")))
    }
  }

}

object PetstoreClientSpec {
  import org.http4s.Uri
  import org.http4s.client.Client
  import cats.effect.IO

  import runtime._

  def withClient(pets: List[Pet] = List.empty)(test: PetstoreClient[IO] => IO[Assertion]): Assertion =
    (for {
      service <- MemoryPetstoreService[IO](pets)
      result <- test(
        PetstoreHttpClient.build(
          Client.fromHttpService(PetstoreEndpoint(service)),
          Uri.unsafeFromString("")
        ))
    } yield result).unsafeRunSync()

}
