package com.bchazalet.cdanslair

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.ListenableFuture
import scala.concurrent.Promise
import com.ning.http.client.Response
import com.ning.http.client.AsyncCompletionHandler
import scala.util.Try
import upickle._

class CdanslairClient(implicit ec: ExecutionContext) {
  import CdanslairClient._
  
  val client = new AsyncHttpClient()

  val mainPage = "http://pluzz.francetv.fr/videos/c_dans_lair.html"
  val info = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz"

  protected def httpGet(url: String): Future[Response] = {
    val promise = Promise.apply[Response]()
    client.prepareGet(url).execute(new AsyncCompletionHandler[Unit](){

        override def onCompleted(response: Response): Unit = {
            promise.complete(Try(response))
        }

        override def onThrowable(t: Throwable) = {
            promise.failure(t)
        }
    })

    promise.future
  }

  protected def getHomepage: Future[String] = httpGet(mainPage).map(_.getResponseBody)

  /** fetches the currently published episodes */
  def fetch(): Future[Seq[Episode]] = {

    getHomepage flatMap { html =>
      val ids = extractIds(html)
      val all = ids.map(this.get(_))
      Future.sequence(all)
    }

  }

  def get(id: EpisodeId): scala.concurrent.Future[Episode] = {
    httpGet(info.format(id.value)).map{r => read[Episode](r.getResponseBody)}
  }

  def close() = {
    client.close()
  }

}

object CdanslairClient {

  /** extract episodes ids from the home page */
  def extractIds(html: String): Seq[EpisodeId] = {
    // <a class="video" id="current_video" href="http://info.francetelevisions.fr/?id-video=121434851" style="display: none;">Voir la vidéo</a>
    //<a href="/videos/c_dans_lair_,121434783.html" class="row"><div class="autre-emission-c4">Emission du 29-04 à 17:46</div><div class="autre-emission-c3">En replay</div></a>
    val latestPattern = """href="http://info.francetelevisions.fr/\?id-video=(\d+)"""".r
    val olderPattern = """href="/videos/c_dans_lair_,(\d+).html"""".r

    val latest = latestPattern.findFirstMatchIn(html).map(_.group(1)).get // there has to be a latest!
    val olderOnes = olderPattern.findAllMatchIn(html).map(_.group(1)).toList
    (olderOnes :+ latest).map(s => EpisodeId(s))
  }

}
