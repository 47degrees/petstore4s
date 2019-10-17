name := "petstore"
val V = new {
  val circe          = "0.11.1"
  val http4s         = "0.20.1"
  val scalatest      = "3.0.5"
  val logbackClassic = "1.2.3"
  val cats           = "1.6.0"
  val catsEffect     = "1.3.0"
  val fs2            = "1.0.4"
}

lazy val commonSettings = Seq(
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.8",
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

lazy val petstore = project
  .in(file("."))
  .settings(commonSettings)
  .aggregate(share, server, client)

lazy val client = project
  .settings(commonSettings)
  .dependsOn(share, server % "test->test")
  .settings(
    idlType := "openapi",
    srcGenSourceDirs := Seq((Compile / resourceDirectory).value),
    srcGenTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    sourceGenerators in Compile += (Compile / srcGen).taskValue,
    idlGenOpenApiHttpImpl := higherkindness.mu.rpc.idlgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-client" % V.http4s,
      ("com.lihaoyi" % "ammonite" % "1.5.0" % "test").cross(CrossVersion.full)
    ) ++ commonDependencies ++ testDependencies ++ logDependencies
  )
  .settings(
    dependencyOverrides ++= overrideDependecies
  )
  .settings(
    sourceGenerators in Test += Def.task {
      val file = (sourceManaged in Test).value / "amm.scala"
      IO.write(file, """object amm extends App { 
        ammonite.Main.main(args) 
      }""")
      Seq(file)
    }.taskValue
  )

lazy val share = project
  .settings(commonSettings)

lazy val server = project
  .settings(commonSettings)
  .dependsOn(share)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-server" % V.http4s
    ) ++ commonDependencies ++ testDependencies ++ logDependencies
  )
  .settings(
    dependencyOverrides ++= overrideDependecies
  )
lazy val overrideDependecies = Seq(
  "org.typelevel" %% "cats-core"   % V.cats,
  "org.typelevel" %% "cats-effect" % V.catsEffect,
  "co.fs2"        %% "fs2-core"    % V.fs2
)
lazy val commonDependencies = Seq(
  "org.http4s" %% "http4s-dsl"    % V.http4s,
  "org.http4s" %% "http4s-circe"  % V.http4s,
  "io.circe"   %% "circe-core"    % V.circe,
  "io.circe"   %% "circe-generic" % V.circe
)

lazy val testDependencies = Seq(
  "org.scalactic" %% "scalactic" % V.scalatest % "test",
  "org.scalatest" %% "scalatest" % V.scalatest % "test"
)

lazy val logDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % V.logbackClassic
)
