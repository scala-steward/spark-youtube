import sbt._

object Dependencies {
  val currentScalaVersion = "2.12.12"

  val scalaLanguage = "org.scala-lang" % "scala-library" % currentScalaVersion
  val scalaCompiler = "org.scala-lang" % "scala-compiler" % currentScalaVersion
  val scalaTest = "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % "test"

  val coreDependencies = Seq(
    scalaLanguage,
    scalaCompiler,
    scalaTest,
    "com.typesafe" % "config" % "1.4.1",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "org.slf4j" % "jcl-over-slf4j" % "1.7.30",
    ("org.apache.hadoop" % "hadoop-common" % "3.3.0")
      .excludeAll(
        ExclusionRule("commons-logging"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("javax.activation"),
        ExclusionRule("javax.servlet"),
        ExclusionRule("io.netty", "netty-common"),
        ExclusionRule("org.apache.curator"),
        ExclusionRule("org.slf4j", "slf4j-log4j12"),
        ExclusionRule("log4j", "log4j")
      ),
    ("org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.3.0")
      .exclude("aopalliance", "aopalliance")
      .exclude("javax.inject", "javax.inject")
      .exclude("org.apache.hadoop", "hadoop-yarn-common")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("log4j", "log4j")
      .exclude("org.apache.hadoop", "hadoop-yarn-client"),
    ("org.apache.spark" %% "spark-streaming" % "3.1.0")
      .excludeAll(
        ExclusionRule("org.apache.hadoop"),
        ExclusionRule("jakarta.xml.bind"),
        ExclusionRule("com.sun.jersey"),
        ExclusionRule("io.netty"),
        ExclusionRule("javax.activation"),
        ExclusionRule("org.slf4j", "slf4j-log4j12"),
        ExclusionRule("log4j", "log4j"),
        ExclusionRule("org.json4s")
      ),
    "com.softwaremill.retry" %% "retry" % "0.3.3",
    ("com.google.apis" % "google-api-services-youtube" % "v3-rev20210210-1.31.0")
      .excludeAll(
        ExclusionRule("commons-logging")
      )
  )

}
