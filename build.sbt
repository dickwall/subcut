name := "subcut"

version := "0.9"

organization := "org.scala-tools"

scalaVersion := "2.9.0-1"

crossScalaVersions := Seq("2.9.0-1", "2.9.0")

// libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.9.0-1" % "compile"
libraryDependencies <+= scalaVersion( "org.scala-lang" % "scala-compiler" % _ )

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "junit" % "junit" % "4.4" % "test"

javacOptions ++= Seq("-Xmx1024M")

scalacOptions += "-deprecation"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

