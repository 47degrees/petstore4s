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

import models._
import cats.effect.concurrent.Ref
import cats.effect.ConcurrentEffect
import cats.implicits._
import petstore.AnotherPetstoreClient._
import shapeless.Coproduct

object MemoryPetstoreService {
  import PetstoreService._
  private def build[F[_]: ConcurrentEffect](ref: Ref[F, List[Pet]]): PetstoreService[F] = new PetstoreService[F] {
    def createPet(newPet: NewPet): F[Either[CreatePetError, Unit]] =
      ref
        .modify[Either[CreatePetError, Unit]](list =>
          if (list.exists(_.name === newPet.name))
            (
              list,
              Coproduct[CreatePetError](
                CreatePetDuplicatedResponseError(s"Pet with name `${newPet.name}` already exists")
              ).asLeft[Unit]
            )
          else
            (
              Pet(list.map(_.id).foldLeft(0L)(Math.max) + 1, newPet.name, newPet.tag) :: list,
              ().asRight[CreatePetError]
            )
        )

    def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]] =
      ref.get
        .map { x =>
          limit.fold(x)(x.take).filter(_.name.contains(name.getOrElse("")))
        }

    def getPet(id: Long): F[Either[GetPetError, Pet]] =
      ref.get
        .map(_.find(_.id === id))
        .map(
          _.fold(Coproduct[GetPetError](GetPetNotFoundResponseError(s"Not found pet with id: $id")).asLeft[Pet])(
            _.asRight[GetPetError]
          )
        )

    def updatePet(id: Long, updatePet: UpdatePet): F[Unit] =
      ref
        .update(list =>
          list.find(_.id === id).map(_.copy(tag = updatePet.tag.some)).fold(list) {
            _ :: list.filter(_.id =!= id)
          }
        )

    def deletePet(id: Long): F[Unit] = ref.update(_.filter(_.id =!= id)).void

  }

  def apply[F[_]: ConcurrentEffect](init: List[Pet] = List.empty): F[PetstoreService[F]] =
    Ref.of[F, List[Pet]](init).map(MemoryPetstoreService.build[F])
}
