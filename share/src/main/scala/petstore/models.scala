package petstore

object models {
  final case class NewPet(name: String)

  final case class Pet(id: Long, name: String)

  final case class Error(code: Int, message: String)
}