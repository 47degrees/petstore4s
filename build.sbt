ThisBuild / organization := "com.47deg"
ThisBuild / scalaVersion := "2.13.2"

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")
addCommandAlias("ci-docs", "documentation/mdoc")

val V = new {
  val circe          = "0.13.0"
  val http4s         = "0.21.3"
  val scalatest      = "3.1.1"
  val logbackClassic = "1.2.3"
  val log4cats       = "1.1.1"
  val cats           = "2.1.1"
}

lazy val petstore = project
  .in(file("."))
  .dependsOn(protocol, server, `client-example`)
  .aggregate(protocol, server, `client-example`)

lazy val protocol = project
  .settings(
    moduleName := "petstore4s-client-example",
    muSrcGenIdlType := higherkindness.mu.rpc.srcgen.Model.IdlType.OpenAPI,
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenIdlTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    sourceGenerators in Compile += (Compile / muSrcGen).taskValue,
    muSrcGenOpenApiHttpImpl := higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
    libraryDependencies ++= Seq(
      "io.circe"   %% "circe-core"          % V.circe,
      "io.circe"   %% "circe-generic"       % V.circe,
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      "org.http4s" %% "http4s-circe"        % V.http4s
    )
  )

lazy val server = project
  .dependsOn(protocol)
  .settings(
    moduleName := "petstore4s-server",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % V.http4s,
      "org.http4s"    %% "http4s-dsl"          % V.http4s,
      "ch.qos.logback" % "logback-classic"     % V.logbackClassic
    )
  )

lazy val `client-example` = project
  .dependsOn(protocol, server % "test->test")
  .settings(
    moduleName := "petstore4s-client",
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-slf4j"  % V.log4cats,
      "ch.qos.logback"     % "logback-classic" % V.logbackClassic,
      "org.scalactic"     %% "scalactic"       % V.scalatest % Test,
      "org.scalatest"     %% "scalatest"       % V.scalatest % Test
    )
  )

lazy val documentation = project
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .enablePlugins(MdocPlugin)
