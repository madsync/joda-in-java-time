[![Build Status](https://travis-ci.com/madsync/joda-in-java-time.svg?branch=master)](https://travis-ci.com/madsync/joda-in-java-time.svg?branch=master)
[![Coverage Status](https://coveralls.io/repos/madsync/joda-in-java-time/badge.png?branch=master)](https://coveralls.io/r/madsync/joda-in-java-time?branch=master)

# joda-in-java-time
This project aims to implement an API as similar as possible to that of the Joda project, which has been in wide use across the Java stack and was recently deprecated in favor of Java's time implementation. 

While vastly superior to Java's original util.Date implementation (what isn't?), the new java time suffers from increased complexity over Joda's API and a difficult migration path in that the API is considerably different. This library will try to bridge that gap in that code written to Joda's interface should be migrateable to Java time at more than 95% with a simple drop-in replacement of

```
org.joda.time => com.madsync.time  
new DateTime(..) => DateTime.apply(..)
```

The project is built in Scala using sbt. Having a Java wrapper (in order to port directly to Java projects) is expected in the future.

The binaries are hosted at sonatype:

```
libraryDependencies += "com.madsync" % "joda-in-java-time" % "0.1.2" withSources()
```