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
  
  val outputFolder = new File("/Users/bchazalet/Downloads/cdanslair")
  
  val download = episodesF.flatMap { episodes =>
    val ep = episodes.head
    val rightFormat = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get //.toRight(s"Could not find a video with the format ${Format.M3U8_DOWNLOAD}")
    println(s"Downloading latest episode: ${ep.id} - ${ep.sous_titre}")
    streamDownloader.download(new URL(rightFormat.url), new File(outputFolder, s"${ep.id.value}.ts"))
  }
  
  try {
    val file = Await.result(download, 3 hours)
  } catch {
    case e: Exception =>  println(e.getMessage)
  } finally {
    client.close()
  }
}



