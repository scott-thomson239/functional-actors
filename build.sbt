scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.3.11",
  "co.fs2" %% "fs2-core" % "3.2.7"
)

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)