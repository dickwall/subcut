import sbt._

class SubCut(info : ProjectInfo) extends DefaultProject(info) with IdeaProject {
  // Repositories
  val jbossRepo = "Jboss Public Repository" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"
  val scalaToolsSnapshots = "Scala Tools Nexus Snapshot" at "http://nexus.scala-tools.org/content/repositories/snapshots/"
  val scalaToolsReleases = "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"

  val scalatest = buildScalaVersion match {
    case "2.8.0" | "2.8.1" => "org.scalatest" % "scalatest" % "1.3" % "test"
    case _                 => "org.scalatest" %% "scalatest" % "1.6.1" % "test"
  }

  // Dependencies
  val junit = "junit" % "junit" % "4.4" % "test"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = scalaToolsSnapshots
  //val publishTo = scalaToolsReleases

  Credentials(Path.userHome / ".ivy2" / ".credentials", log)

  // The following extra settings were copied from the ScalaCheck project definition at
  // http://code.google.com/p/scalacheck/source/browse/tags/1.7/project/build/ScalaCheckProject.scala?r=495

  override def packageDocsJar = defaultJarPath("-javadoc.jar")
  override def packageSrcJar= defaultJarPath("-sources.jar")

  override def packageToPublishActions = super.packageToPublishActions ++ Seq(packageDocs, packageSrc)

  override def deliverScalaDependencies = Nil

  override def documentOptions = Nil

  val sourceArtifact = Artifact(artifactID, "src", "jar", Some("sources"), Nil, None)
  val docsArtifact = Artifact(artifactID, "docs", "jar", Some("javadoc"), Nil, None)


  // Insert extra info into the generated POM
  override def pomExtra =
  <xml:group>
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
}
