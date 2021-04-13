package hu.sztaki.spark.youtube

import hu.sztaki.spark
import hu.sztaki.spark.Factory

class Configuration(silent: Boolean = false)(implicit
factory: Factory.forConfiguration[Configuration])
 extends spark.Configuration[Configuration](
   "stube.conf",
   "stube.defaults.conf",
   true,
   Some("stube"),
   silent
 )
   with Serializable

object Configuration {

  implicit object configurationFactory extends Factory.forConfiguration[Configuration] {

    override def apply(
      fromFile: String,
      fromEnvironment: Boolean,
      restrictTo: Option[String],
      silent: Boolean
    ): Configuration = new Configuration(silent)

  }

}
