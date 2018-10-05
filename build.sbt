scalaVersion := "2.12.6"

name := "webpush-scala"

organization := "com.github.nokamoto"

version := "0.0.1-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("releases")

licenses := Seq(
  "MIT License" -> url("http://www.opensource.org/licenses/mit-license"))

libraryDependencies ++= Seq(
  "com.github.nokamoto" %% "webpush-protobuf" % "0.0.0",
  "com.squareup.okhttp" % "okhttp" % "2.7.5",
  "com.google.crypto.tink" % "apps-webpush" % "1.2.0",
  "com.auth0" % "java-jwt" % "3.4.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.squareup.okhttp" % "mockwebserver" % "2.7.5" % Test,
  "com.typesafe.play" %% "play-json" % "2.6.10" % Test
)

testOptions in Test += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-l",
  "com.github.nokamoto.webpush.WebpushTestingServiceTest")

lazy val WebpushTestingService = config("webpush-testing-service").extend(Test)

configs(WebpushTestingService)

inConfig(WebpushTestingService)(Defaults.testTasks)

testOptions in WebpushTestingService -= Tests.Argument(
  TestFrameworks.ScalaTest,
  "-l",
  "com.github.nokamoto.webpush.WebpushTestingServiceTest")

testOptions in WebpushTestingService += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-n",
  "com.github.nokamoto.webpush.WebpushTestingServiceTest")

useGpg := false

pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)

publishTo := sonatypePublishTo.value

credentials += Credentials("Sonatype Nexus Repository Manager",
                           "oss.sonatype.org",
                           sys.env.getOrElse("SONATYPE_USER", ""),
                           sys.env.getOrElse("SONATYPE_PASS", ""))

sonatypeProfileName := "com.github.nokamoto"

publishMavenStyle := true

sonatypeProjectHosting := Some(
  xerial.sbt.Sonatype
    .GitHubHosting("nokamoto", "webpush-scala", "nokamoto.engr@gmail.com"))

scmInfo := Some(
  ScmInfo(url("https://github.com/nokamoto/webpush-scala"),
          "scm:git@github.com:nokamoto/webpush-scala.git"))

publishConfiguration := publishConfiguration.value.withOverwrite(true)
