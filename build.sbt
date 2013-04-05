organization := "tv.cntt"

name := "xitrum-scalate"

version := "1.0"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

// Most Scala projects are published to Sonatype, but Sonatype is not default
// and it takes several hours to sync from Sonatype to Maven Central
resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "tv.cntt" %% "xitrum" % "1.22" % "provided"

libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"

// For Markdown
libraryDependencies += "org.fusesource.scalamd" %% "scalamd" % "1.6"

// For Scalate to compile CoffeeScript to JavaScript
libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"
