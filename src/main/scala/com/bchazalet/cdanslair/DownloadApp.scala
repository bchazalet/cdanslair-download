package com.bchazalet.cdanslair

import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import java.io.File
import scala.util.Try
import org.joda.time.format.DateTimeFormat

object DownloadApp extends App {
  
  val client = new CdanslairClient()
  
  val episodesF = client.fetch()
  
  val streamDownloader: StreamDownloader = new VLC("/Applications/VLC.app/Contents/MacOS/VLC")
  
  val outputFolder = new File("/Users/bchazalet/Downloads/cdanslair")
  
  val format = DateTimeFormat.forPattern("dd-MM-YYYY")
  
  val undownloaded = episodesF.map { episodes =>
    val eps = episodes.sortWith(_ > _) // most recent first
    val files = outputFolder.listFiles
    def isPresent(ep: Episode): Boolean = files.find(_.getName.startsWith(ep.id.value)).isDefined
    val todo = eps.filter(!isPresent(_))
    println(s"There are ${todo.size} episodes to download:")
    todo.foreach(ep => println(s"${ep.diffusion.publishedAt.toString(format)} -> ${ep.id} - ${ep.sous_titre}"))
    todo
  }
  
  // we should download one after another here, not all at once
  val download = undownloaded.flatMap { episodes =>
    val ep = episodes.head
    val rightFormat = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get //.toRight(s"Could not find a video with the format ${Format.M3U8_DOWNLOAD}")
    println(s"Downloading latest episode: ${ep.id} - ${ep.sous_titre}")
    streamDownloader.download(new URL(rightFormat.url), new File(outputFolder, s"${ep.id.value}-${ep.diffusion.publishedAt.toString(format)}.ts"))
  }
  
  try {
    val file = Await.result(download, 3 hours)
  } catch {
    case e: Exception =>  println(e.getMessage)
  } finally {
    client.close()
  }
}



