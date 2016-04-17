package com.bchazalet.cdanslair

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.ListenableFuture
import scala.concurrent.Promise
import com.ning.http.client.Response
import com.ning.http.client.AsyncCompletionHandler
import scala.util.Try
import play.api.libs.json.Json

class PluzzClient(replay: Replay)(implicit ec: ExecutionContext) {
  import PluzzClient._

  implicit val client = new AsyncHttpClient()

  val info = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz"

  protected def getHomepage: Future[String] = httpGet(replay.mainPage).map(_.getResponseBody)

  /** fetches the currently published episodes */
  def fetch(): Future[Seq[Episode]] = {

    getHomepage flatMap { html =>
      val ids = replay.extract(html)
      val all = ids.map(this.get(_))
      Future.sequence(all)
    }

  }

  def get(id: EpisodeId): scala.concurrent.Future[Episode] = {
    httpGet(info.format(id.value)).flatMap { r =>
      val ep = Episode.epReads.reads(Json.parse(r.getResponseBody)).get
      // hack to select the best defnitions videos, if possible
      val bestVideosF = ep.videos.map(vid => bestDefinition(vid))
      Future.sequence(bestVideosF).map(bestVideos => ep.copy(videos = bestVideos))
    }
  }

  def close() = {
    client.close()
  }

}

object PluzzClient {

  def httpGet(url: String)(implicit client: AsyncHttpClient): Future[Response] = {
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

  /** tries to auto-select the best definition (hack around the fact that now VLC can't pick up the best ones on its own)*/
  def bestDefinition(vid: Video)(implicit ec: ExecutionContext, client: AsyncHttpClient): Future[Video] = {
    if(vid.format == Format.M3U8_DOWNLOAD){
      // download the playlist file, and keep the last line, which should be the best definition
      PluzzClient.httpGet(vid.url).map { r =>
        val lastLine = r.getResponseBody.split("\n").last
        vid.copy(url = lastLine)
      }
    } else {
      Future.successful(vid)
    }
  }

}
