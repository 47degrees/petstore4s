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
        .modify[Either[CreatePetError, Unit]](
          list =>
            if (list.exists(_.name === newPet.name))
              (
                list,
                Coproduct[CreatePetError](
                  CreatePetDuplicatedResponseError(s"Pet with name `${newPet.name}` already exists"))
                  .asLeft[Unit])
            else
              (
                Pet(list.map(_.id).foldLeft(0L)(Math.max) + 1, newPet.name, newPet.tag) :: list,
                ().asRight[CreatePetError])
        )

    def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]] =
      ref.get
        .map { x =>
          limit.fold(x)(x.take).filter(_.name.contains(name.getOrElse("")))
        }

    def getPet(id: Long): F[Either[GetPetError, Pet]] =
      ref.get
        .map(_.find(_.id === id))
        .map(_.fold(Coproduct[GetPetError](GetPetNotFoundResponseError(s"Not found pet with id: $id")).asLeft[Pet])(
          _.asRight[GetPetError]))

    def updatePet(id: Long, updatePet: UpdatePet): F[Unit] =
      ref
        .update(
          list =>
            list.find(_.id === id).map(_.copy(tag = updatePet.tag.some)).fold(list) {
              _ :: list.filter(_.id === id)
          }
        )

    def deletePet(id: Long): F[Unit] = ref.update(_.filter(_.id =!= id)).void

  }

  def apply[F[_]: ConcurrentEffect](init: List[Pet] = List.empty): F[PetstoreService[F]] =
    Ref.of[F, List[Pet]](init).map(MemoryPetstoreService.build[F])
}
