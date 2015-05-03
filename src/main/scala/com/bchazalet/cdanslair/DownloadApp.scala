package com.bchazalet.cdanslair

import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import java.io.File
import scala.util.Try

object DownloadApp extends App {
  
  val client = new CdanslairClient()
  
  val episodesF = client.fetch()
  
  val streamDownloader: StreamDownloader = new VLC("/Applications/VLC.app/Contents/MacOS/VLC")
  
  val download = episodesF.flatMap { episodes =>
    val rightFormat = episodes.head.videos.find(_.format == Format.M3U8_DOWNLOAD).get //.toRight(s"Could not find a video with the format ${Format.M3U8_DOWNLOAD}")
    streamDownloader.download(new URL(rightFormat.url), new File(""))
  }
  
  try {
    val file = Await.result(download, 3 hours)
  } catch {
    case e: Exception =>  println(e.getMessage)
  } finally {
    client.close()
  }
}



