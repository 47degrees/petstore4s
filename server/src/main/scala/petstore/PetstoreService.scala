package petstore

import models._

trait PetstoreService[F[_]] {
  def createPet(newPet: NewPet): F[Unit]

  def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]]

  def getPet(id: Long): F[Option[Pet]]

  def updatePet(id: Long, updatePet: UpdatePet): F[Unit]

  def deletePet(id: Long): F[Unit]
}
