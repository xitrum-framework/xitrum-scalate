organization := "tv.cntt"
name         := "xitrum-scalate"
version      := "2.9.2-SNAPSHOT"

// TODO Support Scala 2.12.13+
// https://github.com/scalate/scalate/issues/309
crossScalaVersions := Seq("2.13.4", "2.12.12")
scalaVersion       := "2.13.4"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// Xitrum 3.26+ requires Java 8
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies += "tv.cntt" %% "xitrum" % "3.30.0" % "provided"

libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.9.6"

// For Markdown
libraryDependencies += "org.scalatra.scalate" %% "scalamd" % "1.7.3"

// Scalate is compatible with a certain versions of scala-compiler:
// https://github.com/scalate/scalate/issues/309
// But we can't force scala-compiler version here, as apps can override it:
//libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value

//------------------------------------------------------------------------------

// Skip API doc generation to speedup "publish-local" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
