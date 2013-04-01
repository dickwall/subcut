name := "subcut"

organization := "com.escalatesoft.subcut"

version := "2.0"

crossScalaVersions := Seq("2.10.0", "2.9.2", "2.9.1", "2.9.0-1", "2.9.0")

scalaVersion := "2.10.1"

scalacOptions += "-deprecation"

libraryDependencies += "junit" % "junit" % "4.5" % "test"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test" cross CrossVersion.full

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
  deps :+ "org.scala-lang" % "scala-compiler" % ver 
}

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

