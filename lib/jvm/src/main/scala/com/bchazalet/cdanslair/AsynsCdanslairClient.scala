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

class AsyncCdanslairClient(implicit ec: ExecutionContext) extends CdanslairClient {

  val client = new AsyncHttpClient()

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
