package hu.sztaki.spark.youtube

import com.sksamuel.elastic4s.Response

import java.util.concurrent.atomic.AtomicInteger
import hu.sztaki.spark.{disqus, Comment, Datum, Elastic, Logger, Thread, Try}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.scheduler.{
  StreamingListener,
  StreamingListenerBatchCompleted,
  StreamingListenerBatchSubmitted
}
import org.apache.spark.streaming.{Seconds, StreamingContext, StreamingContextState}
import org.apache.spark.{SparkConf, SparkContext}
import retry.Success

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls

@SerialVersionUID(-1)
case class Job(outputs: Iterable[DStream[Datum] => Unit])(
  implicit configuration: Configuration,
  disqusConfiguration: disqus.Configuration)
 extends Logger
   with Serializable {
  @transient protected lazy val batch = new SparkContext(new SparkConf())

  @transient protected lazy val streaming =
    new StreamingContext(
      batch,
      Seconds(
        configuration.get[Duration]("stube.spark.streaming.batch-duration").toSeconds
      )
    )

  @transient protected val state = new {
    val batches = new AtomicInteger(0)
    var delay: Option[Long] = None
    var started = false
  }

  protected val & = new Serializable {

    val key = System.getenv().getOrDefault(
      "STUBE_SEARCH_KEY",
      configuration.get[String]("stube.search.key")
    )

    val comment = new Serializable {

      val `maximum-results-per-search` =
        configuration.get[Int]("stube.search.comment.maximum-results-per-search")

      val ordering =
        configuration.get[String]("stube.search.comment.ordering")
          .ensuring(List("time", "relevance").contains(_))

    }

    val output = new Serializable {

      val `elastic-search` = new Serializable {

        val enabled =
          configuration.get[Boolean]("stube.output.elastic-search.enabled")

      }

    }

  }

  streaming.addStreamingListener(new StreamingListener {

    override def onBatchSubmitted(submitted: StreamingListenerBatchSubmitted): Unit = {
      state.batches.incrementAndGet()
      state.batches synchronized {
        state.batches.notifyAll()
      }
    }

    override def onBatchCompleted(batchCompleted: StreamingListenerBatchCompleted): Unit = {
      state.delay = batchCompleted.batchInfo.totalDelay
    }

  })

  initialize()

  def initialize(): Unit = {
    val fetches = streaming
      .receiverStream[Thread](new Receiver(&.key))
      .mapPartitions {
        videos =>
          if (videos.nonEmpty) {
            log.info("Loaded language detector profile. Creating Youtube search provider.")
            val provider = Provider.Cache.get()
            videos.map {
              video =>
                val comments =
                  try {
                    log.info("Performing comment search on Youtube.")
                    provider
                      .commentThreads()
                      .list(java.util.Arrays.asList("id", "replies", "snippet"))
                      .setVideoId(video.thread)
                      .setTextFormat("plainText")
                      .setOrder(&.comment.ordering)
                      .setKey(&.key)
                      .setFields("*")
                      .setMaxResults(&.comment.`maximum-results-per-search`)
                      .execute()
                      .getItems
                      .asScala
                  } catch {
                    case t: Throwable =>
                      log.warn(s"Could not fetch comments for video [${video.thread}], " +
                        s"due to error [${t.getMessage}]!")
                      List()
                  }

                (video, comments)
            }
          } else {
            Iterator.empty
          }
      }
      .flatMap[Datum] {
        case (video, comments) =>
          val extractedComments: Seq[Datum] = comments.flatMap {
            c =>
              try {
                Comment(video, c)
              } catch {
                case t: Throwable =>
                  log.warn(
                    s"Could not convert comment to Comment object of video " +
                      s"[${video.thread}] due to error [${t.getMessage}]!"
                  )
                  t.printStackTrace()
                  Nil
              }
          }

          extractedComments :+ video
      }
      .cache()

    if (&.output.`elastic-search`.enabled) {
      fetches.foreachRDD {
        batch =>
          batch.foreachPartition {
            partition =>
              implicit val insertSuccess = new Success[Response[_]](_.isSuccess)

              val elastic = Elastic.Cache.get()

              Await.result(
                Future.sequence(partition.map {
                  case c: Comment =>
                    elastic.insertAsync(c)
                  case v: Thread =>
                    elastic.insertAsync(v)
                }),
                Int.MaxValue seconds
              )
          }
      }
    }

    outputs.foreach(_.apply(fetches))
  }

  def addStreamingListener(listener: StreamingListener): Unit = {
    log.info("Adding streaming listener.")
    streaming.addStreamingListener(listener)
  }

  def start(block: Boolean = false): Unit = {
    if (streaming.getState() != StreamingContextState.ACTIVE) {
      log.info("Starting up streaming context.")
      state.started = true
      streaming.start()
    }
    if (block) {
      streaming.awaitTermination()
    }
  }

  def awaitProcessing(): Job = {
    start()
    if (state.batches.get() < 1) {
      state.batches synchronized {
        state.batches.wait()
      }
    }
    this
  }

  def kill(): Unit = {
    stop()
    log.info("Killing program.")
    System.exit(0)
  }

  def stop(): Unit = {
    implicit val success = new Success[Unit](_.isInstanceOf[Unit])
    Try.eatAnyShit(Await.result(
      retry.Directly(5) {
        () =>
          Future {
            Await.result(
              Future[Unit] {
                log.warn("Shutting down and destructing existing Spark Streaming engine!")
                streaming.stop(stopSparkContext = true, stopGracefully = false)
                batch.stop()
              },
              30.seconds
            )
          }
      }.recover {
        case _ =>
          log.warn("Could not stop Spark's context correctly in this try!")
          ()
      },
      60.seconds
    )).getOrElse {
      log.error("Could not shut down Spark context correctly! Not attempting again!")
    }
  }

  def processingStarted: Boolean = state.batches.intValue() > 0
}

object Job extends Entrypoint {

  def main(arguments: Array[String]): Unit = {
    Job(List(_.print()))
  }

}
