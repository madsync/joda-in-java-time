import sbt._

import scala.xml.Elem


object Dependencies {
  
  lazy val unifiedLogging: Seq[ModuleID] = Seq(
    // our provided implementation plus redirectors
    "org.slf4j" % "slf4j-api" % Slf4jVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    ScalaLogging, // performant logging API
    //
    "org.slf4j" % "log4j-over-slf4j" % Slf4jVersion,
    "org.slf4j" % "jcl-over-slf4j" % Slf4jVersion,
    "org.slf4j" % "jul-to-slf4j" % Slf4jVersion // ,
    // block the following jars at all costs, 99-empty forces jars with nothing in them. 
    //"log4j" % "log4j" % "99-empty",
    //    "org.slf4j" % "slf4j-log4j12" % "99-empty", // One ring to rule them all !! 
    //"commons-logging" % "commons-logging" % "99-empty" //
  )

  // Versions
  val ScalaVersion = Version.ScalaVersionToUse
  val Slf4jVersion = "1.7.25"

  // Test
  val JUnit: ModuleID = "junit" % "junit" % "4.12" % "test" withSources()
  val Mockito: ModuleID = "org.mockito" % "mockito-core" % "2.7.22" % "test" withSources()
  val ScalaTest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.4" % "test" withSources() exclude("org.scala-lang", "scala-library") exclude("org.scala-lang", "scala-reflect")
  val ScalaTestPlus: ModuleID = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % "test" withSources()

  // Third Party
  val JodaConvert: ModuleID = "org.joda" % "joda-convert" % "1.8.3" withSources()
  val JodaTime: ModuleID = "joda-time" % "joda-time" % "2.9.9" withSources()
  val ParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1" withSources()
  val PlayJson = "com.typesafe.play" %% "play-json" % "2.6.8" withSources() exclude("org.scala-lang", "scala-library") exclude("com.google.guava", "guava")
  val ScalaLogging: ModuleID = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0" withSources()
  val Slf4jApi: ModuleID = "org.slf4j" % "slf4j-api" % Slf4jVersion withSources()

  val globalExclusions: Elem =
    <dependencies>
      <exclude org="javax.jms" module="jms"/>
      <exclude org="com.sun.jdmk" module="jmxtools"/>
      <exclude org="com.sun.jmx" module="jmxri"/>
      <exclude org="org.slf4j" module="slf4j-jdk14"/>
      <exclude org="org.slf4j" module="slf4j-log4j"/>
      <exclude org="org.slf4j" module="slf4j-log4j12"/>
      <exclude org="org.slf4j" module="slf4j-simple"/>
      <exclude org="cglib" module="cglib-nodep"/>
    </dependencies>
}
