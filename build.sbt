import Dependencies._
import scalariform.formatter.preferences._
import sbt.Resolver
import scoverage.ScoverageKeys._

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

def publishDest: Option[Resolver] = {
  val nexus = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  Some("releases"  at nexus)
}

name := "joda-in-java-time"

version := "0.1.1"

scalaVersion := "2.13.0"

developers := List(
  Developer("keith", "Keith Nordstrom", "keith@madsync.com", url("http://keithnordstrom.com"))
)

//
// Org  stuff
organization := "com.madsync"
version := Version.libraryDateVersioning
scalaVersion := Version.ScalaVersionToUse
//
// Compile time optimizations
publishArtifact in(Test, packageBin) := true // Publish tests jarsproject
publishArtifact in(Test, packageSrc) := true // Publish tests-source jars
publishArtifact in(Compile, packageDoc) := false // Disable ScalaDoc generation
publishArtifact in packageDoc := false
publishMavenStyle := true
//
publishTo := publishDest // must use aliases to publish

//
pomExtra :=
  <url>http://www.madsync.com</url>
    <licenses>
      <license>
        <name>Lesser GPL 3.0</name>
        <url>https://opensource.org/licenses/LGPL-3.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/madsync/joda-in-java-time.git</url>
      <connection>scm:https://github.com/madsync/joda-in-java-time.git</connection>
    </scm>
    <developers>
      <developer>
        <id>knordstrom</id>
        <name>Keith Nordstrom</name>
        <url>http://www.timeil.io</url>
      </developer>
      <developer>
        <id>dbuschman7</id>
        <name>David Buschman</name>
        <url>http://www.timeil.io</url>
      </developer>
    </developers>
//
// Compiler configuration
scalacOptions ++= Seq(
  "-deprecation", //
  "-encoding", "UTF-8", //
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-unchecked" // will turn these on one by one to clean up the code base
  //  "-Xfatal-warnings",
  //      "-Xlint"
  //      "-Yno-adapted-args",
  //      "-Ywarn-dead-code",
  //      "-Ywarn-numeric-widen"
  //      "-Ywarn-value-discard",
  //      "-Xfuture" //
  //  "-Ywarn-unused-import"
)
maxErrors := 20

//
// Common Libraries
resolvers += Resolver.jcenterRepo
resolvers += "version99" at "http://version99.qos.ch/" // for logging enforcement

libraryDependencies ++= unifiedLogging ++ Seq(JodaConvert, JodaTime, JUnit, Mockito, PlayJson, ScalaTest)

ivyXML := Dependencies.globalExclusions //

//
// SCoverage settings
coverageEnabled in(Test, test) := true

// Assembly
// ////////////////////
test in assembly := {}

//
// Test settings
logBuffered in Test := true

logLevel in Global := Level.Info
logLevel in Test := Level.Info
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oOF")
//
parallelExecution in Test := false // Need to go sequentially within each test for now
javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.8", "-target", "1.8", "-Xlint") // force java to treat files as UTF-8

//
// Scalaiform settings
scalariformPreferences := scalariformPreferences.value // Scala formatting rules
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 80)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(CompactControlReadability, true)
  .setPreference(SpacesAroundMultiImports, true) //