package petstore

import models._

import fs2.async.Ref
import cats.effect.ConcurrentEffect
import cats.implicits._

object MemoryPetstoreService {
  private def build[F[_]: ConcurrentEffect](ref: Ref[F, List[Pet]]): PetstoreService[F] = new PetstoreService[F] {
    def createPet(newPet: NewPet): F[Unit] =
      ref
        .modify(list => Pet(list.map(_.id).foldLeft(0L)(Math.max) + 1, newPet.name, newPet.tag) :: list)
        .void

    def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]] =
      ref.get
        .map { x =>
          limit.fold(x)(x.take).filter(_.name.contains(name.getOrElse("")))
        }

    def getPet(id: Long): F[Option[Pet]] = ref.get.map(_.find(_.id === id))

    def updatePet(id: Long, updatePet: UpdatePet): F[Unit] =
      ref
        .modify(
          list =>
            list.find(_.id === id).map(_.copy(tag = updatePet.tag.some)).fold(list) {
              _ :: list.filter(_.id === id)
          }
        )
        .void

    def deletePet(id: Long): F[Unit] = ref.modify(_.filter(_.id =!= id)).void

  }

  def apply[F[_]: ConcurrentEffect](init: List[Pet] = List.empty): F[PetstoreService[F]] =
    Ref[F, List[Pet]](init).map(MemoryPetstoreService.build[F])
}
