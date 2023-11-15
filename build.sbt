organization := "de.lolhens"
name := "http4s-brotli"
version := {
  val Tag = "refs/tags/(.*)".r
  sys.env.get("CI_VERSION").collect { case Tag(tag) => tag }
    .getOrElse("0.0.1-SNAPSHOT")
}

scalaVersion := "2.13.12"
crossScalaVersions := Seq("2.12.18", scalaVersion.value)

ThisBuild / versionScheme := Some("early-semver")

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0"))

homepage := scmInfo.value.map(_.browseUrl)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/lhns/http4s-brotli"),
    "scm:git@github.com:lhns/http4s-brotli.git"
  )
)
developers := List(
  Developer(id = "lhns", name = "Pierre Kisters", email = "pierrekisters@gmail.com", url = url("https://github.com/lhns/"))
)

lazy val V = new {
  val betterMonadicFor = "0.3.1"
  val fs2Compress = "1.0.0"
  val http4s = "0.23.24"
}

libraryDependencies ++= Seq(
  "de.lhns" %% "fs2-compress-brotli" % V.fs2Compress,
  "org.http4s" %% "http4s-core"   % V.http4s,
  "org.http4s" %% "http4s-client" % V.http4s,
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)

Compile / doc / sources := Seq.empty

publishMavenStyle := true

publishTo := sonatypePublishToBundle.value

credentials ++= (for {
  username <- sys.env.get("SONATYPE_USERNAME")
  password <- sys.env.get("SONATYPE_PASSWORD")
} yield Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  username,
  password
)).toList
