import ReleaseTransformations._
import sbtrelease.ReleasePlugin.autoImport.releaseStepCommand

lazy val commonSettings = Seq(
  organization := "com.kjetland",
  organizationName := "mbknor",
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq("2.10.4", "2.11.8", "2.12.1"),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
//  publishTo := {
//    val nexus = "http://nexus.nextgentel.net/content/repositories/"
//    if (isSnapshot.value)
//      Some("snapshots" at nexus + "snapshots/")
//    else
//      Some("releases"  at nexus + "thirdparty/")
//  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials_sonatype"),
  homepage := Some(url("https://github.com/mbknor/mbknor-jackson-jsonSchema")),
  licenses := Seq("MIT" -> url("https://github.com/mbknor/mbknor-jackson-jsonSchema/blob/master/LICENSE.txt")),
  startYear := Some(2016),
  pomExtra := (
      <scm>
        <url>git@github.com:mbknor/mbknor-jackson-jsonSchema.git</url>
        <connection>scm:git:git@github.com:mbknor/mbknor-jackson-jsonSchema.git</connection>
      </scm>
      <developers>
        <developer>
          <id>mbknor</id>
          <name>Morten Kjetland</name>
          <url>https://github.com/mbknor</url>
        </developer>
      </developers>),
  compileOrder in Test := CompileOrder.Mixed,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq("-unchecked", "-deprecation"),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)


val jacksonVersion = "2.8.4"
val jacksonModuleScalaVersion = "2.8.4"
val slf4jVersion = "1.7.7"


lazy val deps  = Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "javax.validation" % "validation-api" % "1.1.0.Final",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonModuleScalaVersion % "test",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion % "test",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion % "test",
  "joda-time" % "joda-time" % "2.9.4" % "test",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion % "test"
)

lazy val root = (project in file("."))
  .settings(name := "mbknor-jackson-jsonSchema")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (deps))


releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges,
  releaseStepCommand("sonatypeRelease")
)