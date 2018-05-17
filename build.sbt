name := "subcut"

organization := "com.escalatesoft.subcut"

version := "2.1.1-SNAPSHOT"

crossScalaVersions := Seq("2.12.1", "2.11.7", "2.10.5")

scalaVersion := "2.12.1"

scalacOptions += "-deprecation"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
  deps :+ "org.scala-lang" % "scala-compiler" % ver 
}

publishMavenStyle := false

publishArtifact in Test := false

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

licenses += ("Apache-2.0", url("http://www.apache.org/license/LICENSE-2.0.html"))

site.settings

site.includeScaladoc()

ghpages.settings

git.remoteRepo := "git@github.com:dickwall/subcut.git"
