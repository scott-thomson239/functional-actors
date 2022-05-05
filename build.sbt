scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % "3.3.11",
  "org.scalatest" %% "scalatest" % "3.2.11" % Test
)

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)