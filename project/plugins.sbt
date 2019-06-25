
// Dependencies
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.3")

// Packaging
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.19")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.21")

// Dependency tree plugin
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")

// Code Formatter
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// Linter and code smells
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Assembly
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

// Git helper
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")

addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.1.3")
