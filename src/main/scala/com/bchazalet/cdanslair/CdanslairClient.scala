package com.bchazalet.cdanslair

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class CdanslairClient(implicit ec: ExecutionContext) {
  import CdanslairClient._

  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  val mainPage = "https://www.france.tv/france-5/c-dans-l-air/"
  val info = "https://sivideo.webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s"

  protected def getHomepage: Future[String] = {
    client.url(mainPage).get.map(_.body)
  }

  /** fetches the currently published episodes */
  def fetch(): Future[Seq[Episode]] = {

    getHomepage flatMap { html =>
      val ids = extractIds(html)
      val all = ids.distinct.map(this.get)
      Future.sequence(all).map(_.flatten)
    }

  }

  def get(id: EpisodeId): scala.concurrent.Future[Option[Episode]] = {
    client.url(info.format(id.value)).get.map { r =>
      Episode.epReads.reads(r.json).asOpt
    }
  }

  def close() = {
    client.close()
  }

}

object CdanslairClient {

  /** extract episodes ids from the home page */
  def extractIds(html: String): Seq[EpisodeId] = {
    // <a href="//www.france.tv/france-5/c-dans-l-air/137347-emission-du-vendredi-12-mai-2017.html"
    //     title="C dans l&#039;air"
    //     class="c_black link-all"
    //     data-link="player"
    //     data-video="157542289"
    //     data-video-content="137347">
    val pattern = """data-video="(\d+)"""".r

    val all = pattern.findAllMatchIn(html).map(_.group(1)).toList
    all.map(s => EpisodeId(s))
  }

}
