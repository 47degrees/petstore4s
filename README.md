# Petstore

This project aims to clarify the code to generate (http client and server) based on OpenApi Specification 3.0.0.


## Use
Run the server
```sh
> sbt "server/runMain petstore.MemoryServerApp"
```

Run the client
```sh 
> sbt "~client/test:runMain amm"

@ import petstore._, models._, runtime._, cats.implicits._, org.http4s.Uri, cats.effect.IO, scala.concurrent.ExecutionContext.Implicits.global
@ def program(name: String): IO[List[Pet]] =
        for {
            client <- PetstoreHttpClient[IO](Uri.unsafeFromString("http://localhost:8080"))
            _      <- client.createPet(NewPet(name, none))
            pets   <- client.getPets(none)
        } yield pets
@ program("foo").unsafeRunSync
```