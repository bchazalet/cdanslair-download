package com.bchazalet.cdanslair

/** Represents a series that is available for replay */
case class Replay(name: String, mainPage: String, extract: Html => Seq[EpisodeId])

object Replay {

  implicit def optionToSeq(o: Option[EpisodeId]) = o.toSeq

  def generic(mainPage: String) = Replay("", mainPage, pluzzLatest)

  // <a class="video" id="current_video" href="http://info.francetelevisions.fr/?id-video=121434851" style="display: none;">Voir la vidéo</a>
  val pluzzLatestPattern = """href="http://info.francetelevisions.fr/\?id-video=(\d+)"""".r

  def pluzzLatest(html: Html): Option[EpisodeId] = pluzzLatestPattern.findFirstMatchIn(html).map(_.group(1)).map(s => EpisodeId(s))

  val Cdanslair = {

    /** extract episodes ids from the home page */
    def extractIds(html: String): Seq[EpisodeId] = {
      //<a href="/videos/c_dans_lair_,121434783.html" class="row"><div class="autre-emission-c4">Emission du 29-04 à 17:46</div><div class="autre-emission-c3">En replay</div></a>
      val olderPattern = """href="/videos/c_dans_lair_,(\d+).html"""".r

      val latest = pluzzLatest(html).get // there has to be a latest!
      val olderOnes = olderPattern.findAllMatchIn(html).map(_.group(1)).toList.map(s => EpisodeId(s))
      (olderOnes :+ latest)
    }

    Replay("Cdanslair", "http://pluzz.francetv.fr/videos/c_dans_lair.html", extractIds _)
  }

  val FaitesEntrerLaccuse = Replay("Faites entrer l'accusé", "http://pluzz.francetv.fr/videos/faites_entrer_l_accuse.html", pluzzLatest)

}
