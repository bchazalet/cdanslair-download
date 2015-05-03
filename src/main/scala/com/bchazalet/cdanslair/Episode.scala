package com.bchazalet.cdanslair

import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.JsError
import scala.util.Try
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsPath
import play.api.libs.functional.syntax._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object EpisodeId {
  
  val valid = """^\d+$""".r
  
  implicit val epIdReads = new Reads[EpisodeId]{
    override def reads(json: JsValue) = Try(JsSuccess(EpisodeId(json.as[String]))).getOrElse(JsError("could not parse"))
  }
  
}

case class EpisodeId(value: String){
  require(EpisodeId.valid.findFirstIn(value).isDefined, s"$value does not look like a valid episode id")
}

case class Episode(id: EpisodeId, sous_titre: String, diffusion: Diffusion, videos: Seq[Video]) extends Ordered[Episode]{
  
  def compare(that: Episode) =  this.diffusion.publishedAt.compareTo(that.diffusion.publishedAt)
  
}

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

case class Diffusion(timestamp: Int, publishedAt: DateTime)

object Diffusion {
  
  implicit val diffReads: Reads[Diffusion] = 
    (   
      (JsPath \ "timestamp").read[Int] and
      (JsPath \ "date_debut").read[String].map(s => DateTime.parse(s, DateTimeFormat.forPattern("dd/MM/YYYY HH:mm"))) // "date_debut":"01\/05\/2015 17:50"
    )(Diffusion.apply _)
  
}