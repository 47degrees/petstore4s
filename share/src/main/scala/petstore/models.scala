package petstore

object models {
  final case class NewPet(name: String, tag: Option[String])

  final case class UpdatePet(tag: String)

  final case class Pet(id: Long, name: String, tag: Option[String])

  final case class Error(code: Int, message: String)

  final case class NotFoundError(message: String)

  final case class DuplicatedPetError(message: String)

}
