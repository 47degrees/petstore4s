package petstore

import models._

import shapeless.{:+:, CNil}

trait PetstoreClient[F[_]] {
  import PetstoreClient._

  def createPet(newPet: NewPet): F[Either[CreatePetError, Unit]]

  def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]]

  def getPet(id: Long): F[Either[GetPetError, Pet]]

  def updatePet(id: Long, updatePet: UpdatePet): F[Unit]

  def deletePet(id: Long): F[Unit]
}

object PetstoreClient {
  type GetPetError    = NotFoundError :+: Error :+: CNil
  type CreatePetError = DuplicatedPetError :+: Error :+: CNil
}
