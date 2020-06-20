ThisBuild / organization := "com.47deg"
ThisBuild / scalaVersion := "2.13.2"
ThisBuild / skip in publish := true

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")
addCommandAlias("ci-docs", "mdoc")

lazy val protocol = project
  .settings(
    moduleName := "petstore4s-client-example",
    muSrcGenIdlType := higherkindness.mu.rpc.srcgen.Model.IdlType.OpenAPI,
    muSrcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    muSrcGenIdlTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    sourceGenerators in Compile += (Compile / muSrcGen).taskValue,
    muSrcGenOpenApiHttpImpl := higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
    libraryDependencies ++= Seq(
      "io.circe"   %% "circe-core"          % "0.13.0",
      "io.circe"   %% "circe-generic"       % "0.13.0",
      "org.http4s" %% "http4s-blaze-client" % "0.21.3",
      "org.http4s" %% "http4s-circe"        % "0.21.3"
    )
  )

lazy val server = project
  .dependsOn(protocol)
  .settings(
    moduleName := "petstore4s-server",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % "0.21.3",
      "org.http4s"    %% "http4s-dsl"          % "0.21.3",
      "ch.qos.logback" % "logback-classic"     % "1.2.3"
    )
  )

lazy val `client-example` = project
  .dependsOn(protocol, server % "test->test")
  .settings(
    moduleName := "petstore4s-client",
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-slf4j"  % "1.1.1",
      "ch.qos.logback"     % "logback-classic" % "1.2.3",
      "org.scalactic"     %% "scalactic"       % "3.1.1" % Test,
      "org.scalatest"     %% "scalatest"       % "3.1.1" % Test
    )
  )

lazy val documentation = project
  .settings(mdocOut := file("."))
  .enablePlugins(MdocPlugin)
