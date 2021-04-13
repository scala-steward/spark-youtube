package hu.sztaki.spark.youtube

import hu.sztaki.spark.disqus

trait Entrypoint {
  implicit lazy val configuration: Configuration = new Configuration()
  implicit lazy val disqusConfiguration: disqus.Configuration = new disqus.Configuration()
}
