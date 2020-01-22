val V = new {
  val circe          = "0.11.1"
  val http4s         = "0.20.16"
  val scalatest      = "3.0.8"
  val logbackClassic = "1.2.3"
  val log4cats       = "1.0.1"
  val cats           = "1.6.0"
  val catsEffect     = "1.3.0"
  val fs2            = "1.0.4"
  val scala          = "2.12.10"
}

lazy val petstore = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(protocol, server, `client-example`)

lazy val protocol = project
  .settings(commonSettings)
  .settings(
    moduleName := "petstore4s-client-example",
    idlType := "openapi",
    srcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    idlGenOpenApiHttpImpl := higherkindness.mu.rpc.idlgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
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
      "org.http4s"     %% "http4s-blaze-server" % V.http4s,
      "org.http4s"     %% "http4s-dsl"          % V.http4s,
      "ch.qos.logback" % "logback-classic"      % V.logbackClassic
    )
  )

lazy val `client-example` = project
  .dependsOn(protocol, server % "test->test")
  .settings(commonSettings)
  .settings(
    moduleName := "petstore4s-client",
    libraryDependencies ++= Seq(
      "io.circe"          %% "circe-java8"    % V.circe,
      "io.chrisdavenport" %% "log4cats-slf4j" % V.log4cats,
      "ch.qos.logback"    % "logback-classic" % V.logbackClassic,
      "org.scalactic"     %% "scalactic"      % V.scalatest % Test,
      "org.scalatest"     %% "scalatest"      % V.scalatest % Test
    )
  )

lazy val commonSettings = Seq(
  name := "petstore",
  orgProjectName := "petstore",
  description := "Generating http4s code based on OpenApi Specification 3.0.0.",
  startYear := Option(2019),
  scalaVersion := V.scala,
  crossScalaVersions := Seq(V.scala),
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
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"
  ),
  scalafmtOnCompile := true
)
