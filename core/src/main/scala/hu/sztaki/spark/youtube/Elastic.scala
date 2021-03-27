package hu.sztaki.spark.youtube

import com.sksamuel.elastic4s.ElasticApi.indexInto
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, Response}
import hu.sztaki.spark.youtube.Try.tryHard
import javax.net.ssl.SSLSession
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy
import org.apache.http.ssl.SSLContexts
import org.elasticsearch.client.RestClientBuilder
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, FullTypeHints}
import retry.Success

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Failure
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import scala.concurrent.Future

class Elastic()(implicit configuration: Configuration) extends Logger {

  val & = new Serializable {

    val host =
      configuration.get[String]("stube.output.elastic-search.host")

    val index =
      configuration.get[String]("stube.output.elastic-search.index")

    val `allow-insecure` =
      configuration.get[Boolean]("stube.output.elastic-search.allow-insecure")

  }

  val client =
    if (&.`allow-insecure`) {
      val sslContext = SSLContexts
        .custom()
        .loadTrustMaterial(new TrustSelfSignedStrategy())
        .build()

      object trustAllHostnameVerifier extends javax.net.ssl.HostnameVerifier {
        def verify(h: String, s: SSLSession) = true
      }

      val sslSessionStrategy = new SSLIOSessionStrategy(
        sslContext,
        trustAllHostnameVerifier
      )

      val myHttpAsyncClientCallback = new RestClientBuilder.HttpClientConfigCallback() {
        override def customizeHttpClient(
          httpClientBuilder: HttpAsyncClientBuilder
        ): HttpAsyncClientBuilder = httpClientBuilder.setSSLStrategy(sslSessionStrategy)
      }

      ElasticClient(JavaClient(
        props = ElasticProperties(
          &.host
        ),
        httpClientConfigCallback = myHttpAsyncClientCallback
      ))
    } else {
      ElasticClient(JavaClient(
        props = ElasticProperties(
          &.host
        )
      ))
    }

  implicit val insertSuccess: Success[Response[_]] = new Success[Response[_]](_.isSuccess)

  def insertSync(video: Video): Response[IndexResponse] =
    Await.result(
      insertAsync(video),
      Int.MaxValue seconds
    )

  def insertSync(comment: Comment): Response[IndexResponse] =
    Await.result(
      insertAsync(comment),
      Int.MaxValue seconds
    )

  def insertAsync(comment: Comment): Future[Response[IndexResponse]] =
    retry.Backoff(max = Int.MaxValue)(odelay.Timer.default) {
      () =>
        log.trace("Write attempt of comment [{}] to Elastic.", comment.commentID)
        val f = client.execute {
          indexInto(&.index).doc(
            Serialization.write(comment)(Elastic.formats)
          ).refresh(
            RefreshPolicy.Immediate
          )
        }
        f.onComplete {
          case Failure(exception) =>
            log.trace(
              "Failed to write comment [{}] to Elastic due to error [{}] " +
                "with message [{}]!",
              comment.commentID,
              exception.getClass.getName,
              exception.getMessage
            )
          case util.Success(_) =>
            log.trace("Successful write of comment [{}] to Elastic.", comment.commentID)
        }
        f
    }

  def insertAsync(video: Video): Future[Response[IndexResponse]] =
    retry.Backoff(max = Int.MaxValue)(odelay.Timer.default) {
      () =>
        log.trace("Write attempt of video [{}] to Elastic.", video.videoID)
        val f = client.execute {
          indexInto(&.index).id(video.videoID).doc(
            Serialization.write(video)(Elastic.formats)
          ).refresh(
            RefreshPolicy.Immediate
          )
        }
        f.onComplete {
          case Failure(exception) =>
            log.trace(
              "Failed to write video [{}] to Elastic due to error [{}] " +
                "with message [{}]!",
              video.videoID,
              exception.getClass.getName,
              exception.getMessage
            )
          case util.Success(_) =>
            log.trace("Successful write of video [{}] to Elastic.", video.videoID)
        }
        f
    }

}

object Elastic {

  val formats = DefaultFormats
    .withHints(FullTypeHints(typeHintFieldName = "type", hints = List.empty))

  object Cache {
    protected var elastic: Option[Elastic] = None

    def get()(implicit configuration: Configuration): Elastic =
      synchronized {
        elastic match {
          case Some(e) => e
          case None =>
            elastic = tryHard {
              Some(new Elastic())
            }
            elastic.get
        }
      }

  }

}
