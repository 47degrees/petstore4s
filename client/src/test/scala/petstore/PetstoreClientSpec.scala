package petstore

import org.scalatest._
import Matchers._
import org.scalactic.TypeCheckedTripleEquals
import cats.implicits._
import models._

class PetstoreClientSpec extends FlatSpec with TypeCheckedTripleEquals with EitherValues with OptionValues {
  import PetstoreClientSpec._

  "Petstore client" should "get the pets" in {
    val expectedPets = List(pet(1, "a"), pet(2, "b"), pet(3, "c", "tag1".some))
    withClient(expectedPets) { _.getPets(none, none).map(_ should ===(expectedPets)) }
  }

  it should "get the pets when the limit is established" in {
    val expectedPets = List(pet(1, "a"), pet(2, "b"))
    withClient(expectedPets) { _.getPets(1.some, none).map(_ should ===(expectedPets.take(1))) }
  }

  it should "get the pets when the name is established" in {
    val expectedPets = List(pet(1, "abb"), pet(3, "ma"))
    val pets         = List(pet(2, "bx"), pet(4, "oo")) ++ expectedPets
    withClient(pets) { _.getPets(none, "a".some).map(_ should ===(expectedPets)) }
  }

  it should "get the pets by id" in {
    val expectedPet = pet(1, "a")
    withClient(List(expectedPet, pet(2, "b"))) { _.getPet(1).map(_.right.value should ===(expectedPet)) }
  }

  it should "not get the pets by id when the pet does not exist" in {
    withClient(List(pet(1, "a"), pet(2, "b"))) {
      _.getPet(3).map(_.left.value.select[NotFoundError].value should ===(NotFoundError("Not found pet with id: 3")))
    }
  }

  it should "able to create new pets" in {
    withClient() { client =>
      val petToCreate = newPet("name", "tag".some)
      for {
        _    <- client.createPet(petToCreate)
        pets <- client.getPets(none, none)
      } yield pets should ===(List(pet(1, petToCreate.name, petToCreate.tag)))
    }
  }

  it should "not able to create two pets with the same name" in {
    withClient(List(pet(1, "a", "tag".some))) { client =>
      for {
        result <- client.createPet(newPet("a", "tag".some))
      } yield
        result.left.value.select[DuplicatedPetError].value should ===(
          DuplicatedPetError("Pet with name `a` already exists"))
    }
  }

  it should "able to update pets" in {
    withClient(List(pet(1, "name", "tag".some))) { client =>
      for {
        _         <- client.updatePet(1, UpdatePet("tag1"))
        actualPet <- client.getPet(1)
      } yield actualPet.right.value should ===(pet(1, "name", "tag1".some))
    }
  }

  it should "able to delete pets" in {
    withClient(List(pet(1, "name"))) { client =>
      for {
        _         <- client.deletePet(1)
        actualPet <- client.getPet(1)
      } yield actualPet.left.value.select[NotFoundError].value should ===(NotFoundError("Not found pet with id: 1"))
    }
  }

}

object PetstoreClientSpec {
  import org.http4s.Uri
  import org.http4s.client.Client
  import cats.effect.IO

  import runtime._

  def pet(id: Long, name: String, tag: Option[String] = none): Pet = Pet(id, name, tag)
  def newPet(name: String, tag: Option[String] = none): NewPet     = NewPet(name, tag)

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
