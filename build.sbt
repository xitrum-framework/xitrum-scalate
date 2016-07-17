organization := "tv.cntt"
name         := "xitrum-scalate"
version      := "2.6.1-SNAPSHOT"

scalaVersion       := "2.11.8"
crossScalaVersions := Seq("2.11.8")

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// Xitrum 3.26+ requires Java 8
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies += "tv.cntt" %% "xitrum" % "3.27.0" % "provided"

libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.7.1"

// For Markdown
libraryDependencies += "org.scalatra.scalate" %% "scalamd" % "1.6.1"

//------------------------------------------------------------------------------
// Scalate 1.7.1 uses scala-compiler 2.11.0, which in turn uses scala-reflect 2.11.0.
// Force a newer version, scalaVersion above.
//
// However, Xitrum uses JSON4S, which in turn uses scalap 2.11.0, which in turn
// uses scala-compiler 2.11.0. So by forcing a newer version of scalap in Xitrum,
// we do not have to do anything here.

//libraryDependencies <+= scalaVersion { sv =>
//  "org.scala-lang" % "scala-compiler" % sv
//}

//------------------------------------------------------------------------------

// Skip API doc generation to speedup "publish-local" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
