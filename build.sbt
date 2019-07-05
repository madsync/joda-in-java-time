import Dependencies._
import scalariform.formatter.preferences._
import sbt.Resolver
import scoverage.ScoverageKeys._
import org.scoverage.coveralls.Imports.CoverallsKeys._
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

def publishDest: Option[Resolver] = {
  val nexus = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  Some("releases"  at nexus)
}

name := "joda-in-java-time"

version := "0.1.4"

scalaVersion := Version.ScalaVersionToUse

homepage := Some(url("https://github.com/madsync/joda-in-java-time"))

developers := List(
  Developer("keith", "Keith Nordstrom", "keith@madsync.com", url("http://keithnordstrom.com"))
)

licenses := Seq("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0"))

//
// Org  stuff
organization := "com.madsync"
//version := Version.libraryDateVersioning
scalaVersion := Version.ScalaVersionToUse
//
// Compile time optimizations
publishArtifact in(Test, packageBin) := true // Publish tests jars
publishArtifact in(Test, packageSrc) := true // Publish tests-source jars
publishArtifact in(Compile, packageDoc) := true // Publish ScalaDoc jars
publishArtifact in packageDoc := false
publishMavenStyle := true
//
publishTo := publishDest // must use aliases to publish

coverallsToken := Some("V5OygiTTyLOCH2YuD36AMQs27YZFTA61w")

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("sonatypeOpen \"com.madsync\" \"joda-in-java-time\"", _)),
  ReleaseStep(action = Command.process("publish", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)

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