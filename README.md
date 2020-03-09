[![Build Status](https://travis-ci.org/47degrees/petstore4s.svg?branch=master)](https://travis-ci.org/47degrees/petstore4s)

# Petstore

This project shows how to generate code (an HTTP client and server) based on OpenApi Specification 3.0.0, using [Mu](http://higherkindness.io/mu-scala/).

## Specification

This is the [specification](./protocol/src/main/resources/petstore/petstore.yaml) input, based on a **petstore**, we are using to define the code we want to generate. If you want to view the specification in a more human-readable format, please use the following [editor](https://editor.swagger.io/).

## How to use it

Start the server:

```sh
> sbt "server/run"
```

In a separate terminal, run the client:

```sh
> sbt client-example/run
```

You should see some log output like this:

```
11:09:52.199 [scala-execution-context-global-18] INFO  p.client.Boot - Pet = Right(Pet(1,Beethoven,None))
11:09:52.255 [scala-execution-context-global-18] INFO  p.client.Boot - My pets = List(Pet(2,Meadow,Some(female-dog)), Pet(1,Beethoven,None))
11:09:52.281 [scala-execution-context-global-18] INFO  p.client.Boot - No existing pet = Left(Inl(GetPetNotFoundResponseError(Not found pet with id: -1)))
11:09:52.306 [scala-execution-context-global-18] INFO  p.client.Boot - My new pet list = List(Pet(1,Beethoven,Some(updated)), Pet(2,Meadow,Some(female-dog)))
11:09:52.313 [scala-execution-context-global-18] INFO  p.client.Boot - Updated Pet = Right(Pet(1,Beethoven,Some(updated)))
11:09:52.314 [scala-execution-context-global-18] INFO  p.client.Boot - Deleting pet with id 1 from the catalogue...
11:09:52.337 [scala-execution-context-global-18] INFO  p.client.Boot - My pet list updated = List(Pet(2,Meadow,Some(female-dog)))
```

## Generated code and hand-written code

Mu's sbt plugin parses the OpenAPI specification file and generates Scala code
for:

* The request and response models
* An HTTP client based on http4s

If you want to inspect the generated code, run `sbt compile` and then take a
look in the `protocol/target/scala-2.12/src_managed/` directory.

As a user, you need to write the code for the HTTP server, making use of the
generated model classes. Take a look at the `server` project for an example of
how to do that.
