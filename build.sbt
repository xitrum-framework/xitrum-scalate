organization := "tv.cntt"

name := "xitrum-scalate"

version := "1.4-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// http://www.scala-sbt.org/release/docs/Detailed-Topics/Java-Sources
// Avoid problem when this lib is built with Java 7 but the projects that use it
// are run with Java 6
// java.lang.UnsupportedClassVersionError: Unsupported major.minor version 51.0
javacOptions ++= Seq("-source", "1.6", "-target", "1.6")

// Most Scala projects are published to Sonatype, but Sonatype is not default
// and it takes several hours to sync from Sonatype to Maven Central
resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "tv.cntt" %% "xitrum" % "2.13-SNAPSHOT" % "provided"

libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"

// For Markdown
libraryDependencies += "org.fusesource.scalamd" %% "scalamd" % "1.6"

//------------------------------------------------------------------------------
// Scalate 1.6.1 uses scala-compiler 2.10.0, which in turn uses scala-reflect 2.10.0.
// Force a newer version, scalaVersion above.
//
// However, Xitrum uses JSON4S, which in turn uses scalap 2.10.0, which in turn
// uses scala-compiler 2.10.0. So by forcing a newer version of scalap in Xitrum,
// we do not have to do anything here.

//libraryDependencies <+= scalaVersion { sv =>
//  "org.scala-lang" % "scala-compiler" % sv
//}

//------------------------------------------------------------------------------

// Skip API doc generation to speedup "publish-local" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
