ThisBuild / organization    := "com.47deg"
ThisBuild / scalaVersion    := "2.13.6"
ThisBuild / skip in publish := true
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/47degrees/petstore4s"),
    "scm:git:https://github.com/47degrees/petstore4s.git",
    Some("scm:git:git@github.com:47degrees/petstore4s.git")
  )
)

addCommandAlias("ci-test", "scalafmtCheckAll; scalafmtSbtCheck; test")
addCommandAlias("ci-docs", "github; mdoc")
addCommandAlias("ci-publish", "compile")

lazy val protocol = project
  .settings(
    muSrcGenIdlType      := higherkindness.mu.rpc.srcgen.Model.IdlType.OpenAPI,
    muSrcGenSourceDirs   := Seq((Compile / resourceDirectory).value),
    muSrcGenIdlTargetDir := (Compile / sourceManaged).value / "compiled_openapi",
    sourceGenerators in Compile += (Compile / muSrcGen).taskValue,
    muSrcGenOpenApiHttpImpl := higherkindness.mu.rpc.srcgen.openapi.OpenApiSrcGenerator.HttpImpl.Http4sV20,
    addCompilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.6" cross CrossVersion.full),
    scalacOptions += "-P:silencer:pathFilters=src_managed",
    libraryDependencies ++= Seq(
      "io.circe"   %% "circe-core"          % "0.14.1",
      "io.circe"   %% "circe-generic"       % "0.14.1",
      "org.http4s" %% "http4s-blaze-client" % "0.21.3",
      "org.http4s" %% "http4s-circe"        % "0.21.3"
    )
  )
  .enablePlugins(SrcGenPlugin)

lazy val server = project
  .dependsOn(protocol)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-blaze-server" % "0.21.3",
      "org.http4s"    %% "http4s-dsl"          % "0.21.3",
      "ch.qos.logback" % "logback-classic"     % "1.2.6"
    )
  )

lazy val `client-example` = project
  .dependsOn(protocol, server % "test->test")
  .settings(
    libraryDependencies ++= Seq(
      "io.chrisdavenport" %% "log4cats-slf4j"  % "1.1.1",
      "ch.qos.logback"     % "logback-classic" % "1.2.6",
      "org.scalactic"     %% "scalactic"       % "3.2.9" % Test,
      "org.scalatest"     %% "scalatest"       % "3.2.9" % Test
    )
  )

lazy val documentation = project
  .settings(mdocOut := file("."))
  .enablePlugins(MdocPlugin)
