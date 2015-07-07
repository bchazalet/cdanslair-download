package com.bchazalet.cdanslair

import scala.util.Try
import upickle.Js
import upickle.key

object EpisodeId {

  val valid = """^\d+$""".r

  implicit val epIdReader = upickle.Reader[EpisodeId]{
    case Js.Str(str) => EpisodeId(str)
  }

}

case class EpisodeId(value: String){
  require(EpisodeId.valid.findFirstIn(value).isDefined, s"$value does not look like a valid episode id")
}

case class Episode(id: EpisodeId, sous_titre: String, diffusion: Diffusion, videos: Seq[Video])

case class Video(format: String, url: String, statut: String)

object Format {

  val HDS_AKAMAI = "hds_akamai"
  val HLS_V5_OS = "hls_v5_os"
  val M3U8_DOWNLOAD = "m3u8-download"

}

case class Diffusion(timestamp: Int, @key("date_debut") startDate: String)
