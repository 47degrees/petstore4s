package petstore

import models._
import petstore.AnotherPetstoreClient._
import shapeless.{:+:, CNil}

trait PetstoreService[F[_]] {
  import PetstoreService._
  def createPet(newPet: NewPet): F[Either[CreatePetError, Unit]]

  def getPets(limit: Option[Int], name: Option[String]): F[List[Pet]]

  def getPet(id: Long): F[Either[GetPetError, Pet]]

  def updatePet(id: Long, updatePet: UpdatePet): F[Unit]

  def deletePet(id: Long): F[Unit]
}

object PetstoreService {
  type GetPetError    = GetPetNotFoundResponseError :+: PetError :+: CNil
  type CreatePetError = CreatePetDuplicatedResponseError :+: PetError :+: CNil
}
