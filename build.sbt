addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")
addCommandAlias("ci-docs", "project-docs/mdoc")

val V = new {
  val circe          = "0.13.0"
  val http4s         = "0.21.3"
  val scalatest      = "3.1.1"
  val logbackClassic = "1.2.3"
  val log4cats       = "1.1.1"
  val cats           = "2.1.1"
  val scala          = "2.13.1"
}

lazy val petstore = project
  .in(file("."))
  .settings(commonSettings)
  .dependsOn(protocol, server, `client-example`)
  .aggregate(protocol, server, `client-example`)

lazy val protocol = project
  .settings(commonSettings)
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
  .settings(commonSettings)
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
  .settings(commonSettings)
  .settings(
    moduleName := "petstore4s-client",
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-slf4j"  % V.log4cats,
      "ch.qos.logback"     % "logback-classic" % V.logbackClassic,
      "org.scalactic"     %% "scalactic"       % V.scalatest % Test,
      "org.scalatest"     %% "scalatest"       % V.scalatest % Test
    )
  )

lazy val `project-docs` = (project in file(".docs"))
  .dependsOn(petstore)
  .aggregate(petstore)
  .settings(commonSettings)
  .settings(moduleName := "petstore4s-project-docs")
  .settings(mdocIn := file(".docs"))
  .settings(mdocOut := file("."))
  .settings(skip in publish := true)
  .enablePlugins(MdocPlugin)

lazy val commonSettings = Seq(
  organization := "com.47deg",
  scalaVersion := V.scala,
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )
)
