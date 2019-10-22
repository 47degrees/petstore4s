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

import cats.effect._
import io.chrisdavenport.log4cats.SelfAwareStructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s._
import org.http4s.client.blaze.{BlazeClientConfig, Http1Client}
import petstore.models.{NewPet, UpdatePet}

object Boot {
  lazy val L: SelfAwareStructuredLogger[IO] = Slf4jLogger.unsafeCreate

  def main(args: Array[String]): Unit = {

    Http1Client[IO](config = BlazeClientConfig.defaultConfig)
      .map(AnotherPetstoreHttpClient.build(_, Uri.uri("http://localhost:8080")))
      .flatMap { petStoreClient =>
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
        } yield ())
      }
      .unsafeRunSync

  }

}
