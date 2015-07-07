package com.bchazalet.cdanslair

import scala.concurrent.{ ExecutionContext, Future }

trait CdanslairClient {

  implicit val ec: ExecutionContext

  val mainPage = "http://pluzz.francetv.fr/videos/c_dans_lair.html"

  val info = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz"

  /** extract episodes ids from the home page */
  protected[this] def extractIds(html: String): Seq[EpisodeId] = {
    // <a class="video" id="current_video" href="http://info.francetelevisions.fr/?id-video=121434851" style="display: none;">Voir la vidéo</a>
    //<a href="/videos/c_dans_lair_,121434783.html" class="row"><div class="autre-emission-c4">Emission du 29-04 à 17:46</div><div class="autre-emission-c3">En replay</div></a>
    val latestPattern = """href="http://info.francetelevisions.fr/\?id-video=(\d+)"""".r
    val olderPattern = """href="/videos/c_dans_lair_,(\d+).html"""".r

    val latest = latestPattern.findFirstMatchIn(html).map(_.group(1)).get // there has to be a latest!
    val olderOnes = olderPattern.findAllMatchIn(html).map(_.group(1)).toList
    (olderOnes :+ latest).map(s => EpisodeId(s))
  }

  /** fetches the currently published episodes */
  def fetch(): Future[Seq[Episode]] = {

    getHomepage flatMap { html =>
      val ids = extractIds(html)
      val all = ids.map(this.get(_))
      Future.sequence(all)
    }

  }

  protected[this] def getHomepage(): Future[String]

  def get(id: EpisodeId): Future[Episode]

}
