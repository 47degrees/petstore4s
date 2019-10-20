# Petstore

This project shows how to generate code (http client and server) based on OpenApi Specification 3.0.0.

## Specification

This is the [specification](./protocol/src/main/resources/petstore/petstore.yaml) input, base on a **petstore**, we are using to define the code we want to generate. If you want to view the specification, please use the follow [editor](https://editor.swagger.io/).

## How to use it

Run the server:

```sh
> sbt "server/run"
```

Run the client:

```sh 
> sbt client-example/run
