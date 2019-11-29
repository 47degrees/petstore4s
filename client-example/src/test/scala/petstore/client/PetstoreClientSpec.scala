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
package client

import cats.implicits._
import org.scalactic.TypeCheckedTripleEquals
import matchers.should.Matchers._
import org.scalatest._
import petstore.AnotherPetstoreClient.{CreatePetDuplicatedResponseError, GetPetNotFoundResponseError}
import petstore.models.{NewPet, Pet, UpdatePet}
import petstore.{AnotherPetstoreClient, AnotherPetstoreHttpClient, MemoryPetstoreService, PetstoreEndpoint}

import scala.concurrent.ExecutionContext
import org.scalatest.matchers
import org.scalatest.flatspec.AnyFlatSpec

class PetstoreClientSpec extends AnyFlatSpec with TypeCheckedTripleEquals with EitherValues with OptionValues {
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
      _.getPet(3).map(
        _.left.value.select[GetPetNotFoundResponseError].value should ===(
          GetPetNotFoundResponseError("Not found pet with id: 3")
        )
      )
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
      } yield result.left.value.select[CreatePetDuplicatedResponseError].value should ===(
        CreatePetDuplicatedResponseError("Pet with name `a` already exists")
      )
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
      } yield actualPet.left.value.select[GetPetNotFoundResponseError].value should ===(
        GetPetNotFoundResponseError("Not found pet with id: 1")
      )
    }
  }
}

object PetstoreClientSpec {
  import cats.effect.IO
  import org.http4s.Uri
  import org.http4s.client.Client
  import org.http4s.implicits._
  import org.http4s.server.Router

  private implicit val cs = IO.contextShift(ExecutionContext.global)

  def pet(id: Long, name: String, tag: Option[String] = none): Pet = Pet(id, name, tag)
  def newPet(name: String, tag: Option[String] = none): NewPet     = NewPet(name, tag)

  def withClient(pets: List[Pet] = List.empty)(test: AnotherPetstoreClient[IO] => IO[Assertion]): Assertion =
    (for {
      service <- MemoryPetstoreService[IO](pets)
      result <- test(
        AnotherPetstoreHttpClient.build(
          Client.fromHttpApp(Router(("/", PetstoreEndpoint(service))).orNotFound),
          Uri.unsafeFromString("")
        )
      )
    } yield result).unsafeRunSync()
}
