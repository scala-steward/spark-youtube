package hu.sztaki.spark.youtube

import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.duration.DurationInt

class suiteElasticOutput extends stubeFunSpec {
  configuration = configuration.set[Boolean]("stube.output.elastic-search.enabled", true)

  val elastic = Elastic.Cache.get()

  ignore("The Youtube Spark job") {
    var job: Option[Job] = None
    var results: Iterable[Datum] = Iterable.empty

    it("should be able to get created,") {
      job = Some(new Job(List(_.foreachRDD {
        datum => results = results ++ datum.collect()
      })))
    }
    it("should be able to get started,") {
      job.get.start()
    }
    it("should be able to finish with first batch job,") {
      job.get.awaitProcessing()
    }
    it("should eventually fetch some data,") {
      eventually {
        results.size should be >= 1
        results.foreach {
          datum =>
            val getResponse = elastic.client.execute(
              get(elastic.&.index, datum.videoID)
            ).await(10 seconds).result
            getResponse.found should be(true)
        }
      }
    }
  }
}
