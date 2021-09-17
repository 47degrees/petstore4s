/*
 * Copyright 2019-2020 47 Degrees, LLC. <http://www.47deg.com>
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

import cats.effect._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import petstore.models.{NewPet, UpdatePet}

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
        _              <- petStoreClient.createPet(NewPet("Beethoven", None))
        beethoven      <- petStoreClient.getPet(petId = 1)
        _              <- L.info(s"Pet = $beethoven")
        _              <- petStoreClient.createPet(NewPet("Meadow", Some("female-dog")))
        myPets         <- petStoreClient.getPets(limit = None, name = None)
        _              <- L.info(s"My pets = $myPets")
        nonExistingPet <- petStoreClient.getPet(petId = -1)
        _              <- L.info(s"No existing pet = $nonExistingPet")
        _              <- petStoreClient.updatePet(petId = 1, updatePet = UpdatePet("updated"))
        myNewPetList   <- petStoreClient.getPets(limit = None, name = None)
        _              <- L.info(s"My new pet list = $myNewPetList")
        newBeethoven   <- petStoreClient.getPet(petId = 1)
        _              <- L.info(s"Updated Pet = $newBeethoven")
        _              <- L.info(s"Deleting pet with id 1 from the catalogue...")
        _              <- petStoreClient.deletePet(petId = 1)
        myPets         <- petStoreClient.getPets(limit = None, name = None)
        _              <- L.info(s"My pet list updated = $myPets")
      } yield ExitCode.Success)
    }
  }
}
