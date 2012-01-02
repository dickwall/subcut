name := "subcut"

organization := "org.scala-tools.subcut"

version := "1.0"

crossScalaVersions := Seq("2.9.1", "2.9.0-1", "2.9.0", "2.8.0", "2.8.1")

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "junit" % "junit" % "4.5" % "test"

libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
  deps :+ "org.scala-lang" % "scala-compiler" % ver
}

publishTo <<= (version) { version: String =>
  val scalaTools = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at scalaTools + "snapshots/")
  else Some("releases" at scalaTools + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

pomExtra := <xml:group>
    <inceptionYear>2011</inceptionYear>
    <name>Scala Uniquely Bound Classes Under Traits: SubCut</name>
    <description>
      A simple, lightweight and convenient way to inject dependencies
      in scala, in a scala-like way.
    </description>
    <url>http://scala-tools.org/mvnsites/subcut</url>
    <organization>
      <name>scala-tools.org</name>
      <url>http://scala-tools.org/mvnsites/subcut/</url>
    </organization>
    <licenses>
      <license>
        <name>Apache License, ASL Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>dickwall</id>
        <name>Dick Wall</name>
        <timezone>-8</timezone>
        <email>dwall [at] bldc.org</email>
        <roles>
          <role>BDFL</role>
        </roles>
      </developer>
    </developers>
    <issueManagement>
      <system>GitHub</system>
      <url>http://github.com/dickwall/subcut/issues</url>
    </issueManagement>
    <scm>
      <connection>scm:git://github.com/dickwall/subcut.git</connection>
      <url>http://github.com/dickwall/subcut/tree/master</url>
    </scm>
  </xml:group>

