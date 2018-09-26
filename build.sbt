scalaVersion := "2.12.6"

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
