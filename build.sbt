organization := "tv.cntt"

name := "xitrum-scalate"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

libraryDependencies += "tv.cntt" %% "xitrum" % "1.20" % "provided"

libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"

// For Markdown
libraryDependencies += "org.fusesource.scalamd" %% "scalamd" % "1.6"

// For Scalate to compile CoffeeScript to JavaScript
libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"
