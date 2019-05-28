package petstore

import models._

trait PetstoreService[F[_]] {
  def createPet(newPet: NewPet): F[Unit]

  def getPets(limit: Option[Int]): F[List[Pet]]

  def getPet(id: Long): F[Option[Pet]]
}
