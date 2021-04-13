package hu.sztaki.spark.youtube

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Response
import hu.sztaki.spark.{Datum, Elastic}
import org.apache.commons.lang3.RandomStringUtils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

class suiteElasticOutput extends stubeFunSpec {
  configuration = configuration.set[Boolean]("stube.output.elastic-search.enabled", true)
  val randomIndex = RandomStringUtils.randomAlphabetic(8).toLowerCase

  disqusConfiguration = disqusConfiguration
    .set[Boolean]("squs.output.elastic-search.enabled", true)
    .set[String]("squs.output.elastic-search.index", randomIndex)

  val elastic = Elastic.Cache.get()

  def awaitSuccess[T](future: Future[Response[T]]): T =
    eventually {
      val result = Await.result(
        future,
        20 seconds
      )
      if (result.isError) {
        log.error("Elasticsearch execute failed with [{}]!", result.error.reason)
      }
      result.isSuccess should be(true)
      result.result
    }

  describe("The Youtube Spark job") {
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
              get(elastic.&.index, datum.ID.stringify)
            ).await(10 seconds).result
            getResponse.found should be(true)
        }
      }
    }
    it("should be able to drop test index,") {
      awaitSuccess(
        elastic.client.execute(
          deleteIndex(randomIndex)
        )
      )
    }
  }
}
