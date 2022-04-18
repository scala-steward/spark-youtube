import sbt.Keys.libraryDependencies
import sbt.{ExclusionRule, _}

object Dependencies {
  val currentScalaVersion = "2.13.8"

  val coreDependencies = Seq(
    "org.scala-lang" % "scala-library" % currentScalaVersion,
    "org.scala-lang" % "scala-compiler" % currentScalaVersion,
    "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % "test",
    "com.typesafe" % "config" % "1.4.2",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
    ("hu.sztaki.spark.squs" %% "core" % "0.0.16")
      .exclude("com.squareup.okio", "okio"),
    ("org.apache.hadoop" % "hadoop-common" % "3.3.2")
      .excludeAll(
        ExclusionRule("commons-logging"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("javax.activation"),
        ExclusionRule("javax.servlet"),
        ExclusionRule("io.netty"),
        ExclusionRule("org.apache.curator"),
        ExclusionRule("log4j", "log4j"),
        ExclusionRule("org.slf4j", "slf4j-log4j12")
      ),
    ("org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.3.2")
      .exclude("aopalliance", "aopalliance")
      .exclude("javax.inject", "javax.inject")
      .exclude("org.apache.hadoop", "hadoop-yarn-common")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("log4j", "log4j")
      .exclude("com.squareup.okio", "okio")
      .exclude("org.apache.hadoop", "hadoop-yarn-client")
      .excludeAll(
        ExclusionRule("io.netty")
      ),
    ("org.apache.spark" %% "spark-streaming" % "3.2.1")
      .excludeAll(
        ExclusionRule("org.apache.hadoop"),
        ExclusionRule("jakarta.xml.bind"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("io.netty", "netty-common"),
        ExclusionRule("io.netty", "netty-resolver"),
        ExclusionRule("io.netty", "netty-handler"),
        ExclusionRule("io.netty", "netty-codec"),
        ExclusionRule("io.netty", "netty-transport"),
        ExclusionRule("io.netty", "netty-buffer"),
        ExclusionRule("io.netty", "netty-transport-native-unix-common"),
        ExclusionRule("io.netty", "netty-transport-native-epoll"),
        ExclusionRule("javax.activation"),
        ExclusionRule("org.slf4j", "slf4j-log4j12")
      ),
    "com.softwaremill.retry" %% "retry" % "0.3.4",
    ("com.google.apis" % "google-api-services-youtube" % "v3-rev20220409-1.32.1")
      .excludeAll(
        ExclusionRule("commons-logging")
      ),
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.17.2",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.2",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.10.5",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.10.5",
    "com.sksamuel.elastic4s" % "elastic4s-core_2.12" % "8.1.0",
    ("com.sksamuel.elastic4s" % "elastic4s-client-esjava_2.12" % "8.1.0")
      .excludeAll(
        ExclusionRule("commons-logging")
      ),
    "com.softwaremill.retry" %% "retry" % "0.3.4"
  )

}
