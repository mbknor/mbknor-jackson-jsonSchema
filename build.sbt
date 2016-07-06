
lazy val commonSettings = Seq(
  organization := "com.kjetland",
  organizationName := "mbknor",
  version := "1.0.1-build4-SNAPSHOT",
  scalaVersion := "2.11.8",
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
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
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
  scalacOptions ++= Seq("-unchecked", "-deprecation")
)


val jacksonVersion = "2.7.5"
val jacksonModuleScalaVersion = "2.7.4"
val slf4jVersion = "1.7.7"


lazy val deps  = Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "javax.validation" % "validation-api" % "1.1.0.Final",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
  "com.github.fge" % "json-schema-validator" % "2.2.6" % "test",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % jacksonModuleScalaVersion % "test"
)

lazy val root = (project in file("."))
  .settings(name := "mbknor-jackson-jsonSchema")
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= (deps))

