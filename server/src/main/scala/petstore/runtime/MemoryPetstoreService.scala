package petstore

import models._

import fs2.async.Ref
import cats.effect.ConcurrentEffect
import cats.implicits._

object MemoryPetstoreService {

  private def build[F[_]: ConcurrentEffect](ref: Ref[F, List[Pet]]): PetstoreService[F] = new PetstoreService[F] {
    def createPet(newPet: NewPet): F[Unit] =
      ref
        .modify(list => Pet(list.map(_.id).foldLeft(0L)(Math.max) + 1, newPet.name) :: list)
        .void

    def getPets(limit: Option[Int]): F[List[Pet]] = ref.get.map(x => limit.fold(x)(x.take))

    def getPet(id: Long): F[Option[Pet]] = ref.get.map(_.find(_.id == id))
  }

  def apply[F[_]: ConcurrentEffect](init: List[Pet]): F[PetstoreService[F]] =
    Ref[F, List[Pet]](init).map(MemoryPetstoreService.build[F])
}
