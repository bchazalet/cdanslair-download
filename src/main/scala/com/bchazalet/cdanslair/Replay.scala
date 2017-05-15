package com.bchazalet.cdanslair

/** Represents a series that is available for replay */
case class Replay(name: String, mainPage: String, extract: Html => Seq[EpisodeId])

object Replay {

  implicit def optionToSeq(o: Option[EpisodeId]) = o.toSeq

  def generic(mainPage: String) = Replay("", mainPage, pluzz)

  // <a href="//www.france.tv/france-5/c-dans-l-air/137347-emission-du-vendredi-12-mai-2017.html"
  //     title="C dans l&#039;air"
  //     class="c_black link-all"
  //     data-link="player"
  //     data-video="157542289"
  //     data-video-content="137347">
  val pattern = """data-video="(\d+)"""".r

  def pluzz(html: Html): Seq[EpisodeId] = pattern.findAllMatchIn(html).map(_.group(1)).toList.map(s => EpisodeId(s))

  val Cdanslair = Replay("Cdanslair", "https://www.france.tv/france-5/c-dans-l-air/", pluzz)

  val FaitesEntrerLaccuse = Replay("Faites entrer l'accusé", "https://www.france.tv/france-2/faites-entrer-l-accuse/", pluzz)

}
