scalaVersion := "2.12.6"

organization := "com.github.nokamoto"

version := "0.0.0-SNAPSHOT"

PB.protoSources in Compile := (file("webpush-protobuf/webpush/protobuf").getCanonicalFile * AllPassFilter).get

PB.includePaths in Compile := Seq(file("webpush-protobuf").getCanonicalFile,
                                  target.value / "protobuf_external")

PB.targets in Compile := Seq(
  scalapb
    .gen(grpc = false, flatPackage = true) -> (sourceManaged in Compile).value)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
  "com.squareup.okhttp" % "okhttp" % "2.7.5",
  "com.google.crypto.tink" % "apps-webpush" % "1.2.0",
  "com.auth0" % "java-jwt" % "3.4.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "com.squareup.okhttp" % "mockwebserver" % "2.7.5" % Test,
  "com.typesafe.play" %% "play-json" % "2.6.10" % Test
)

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest,
                                      "-l",
                                      "com.github.nokamoto.webpush.FirefoxTest")

lazy val Firefox = config("firefox").extend(Test)

configs(Firefox)

inConfig(Firefox)(Defaults.testTasks)

testOptions in Firefox -= Tests.Argument(
  TestFrameworks.ScalaTest,
  "-l",
  "com.github.nokamoto.webpush.FirefoxTest")

testOptions in Firefox += Tests.Argument(
  TestFrameworks.ScalaTest,
  "-n",
  "com.github.nokamoto.webpush.FirefoxTest")

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
