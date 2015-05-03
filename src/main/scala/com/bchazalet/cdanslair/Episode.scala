package com.bchazalet.cdanslair

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.JsError
import scala.util.Try
import play.api.libs.json.JsSuccess

object EpisodeId {
  
  val valid = """^\d+$""".r
  
  implicit val epIdReads = new Reads[EpisodeId]{
    override def reads(json: JsValue) = Try(JsSuccess(EpisodeId(json.as[String]))).getOrElse(JsError("could not parse"))
  }
  
}

case class EpisodeId(value: String){
  require(EpisodeId.valid.findFirstIn(value).isDefined, s"$value does not look like a valid episode id")
}

case class Episode(id: EpisodeId, sous_titre: String, diffusion: Diffusion, videos: Seq[Video])

object Episode {
  private implicit val v = Video.videoReads
  private implicit val d = Diffusion.diffReads
  
  implicit val epReads = Json.reads[Episode]
  
}


case class Video(format: String, url: String, statut: String)

object Video {
  
  implicit val videoReads = Json.reads[Video]
  
}

object Format {
  
  val HDS_AKAMAI = "hds_akamai"
  val HLS_V5_OS = "hls_v5_os"
  val M3U8_DOWNLOAD = "m3u8-download"
  
}

case class Diffusion(timestamp: Int, date_debut: String)

object Diffusion {
  
  implicit val diffReads = Json.reads[Diffusion]
  
}