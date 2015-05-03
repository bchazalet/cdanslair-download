package com.bchazalet.cdanslair

import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import java.io.File
import scala.util.Try
import scala.sys.process.Process

object DownloadApp extends App {
  
  // fetch the main page and parse its html
  val client = new CdanslairClient()
  val mainPage = client.getMainPage
  
  val episodesF = mainPage flatMap { html =>
    
    val ids = Parsing.extractIds(html)
    
    val all = ids.map(client.get(_))
    
    Future.sequence(all)
    
  }
  
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

class CdanslairClient {
  val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  val client = new play.api.libs.ws.ning.NingWSClient(builder.build())
  
  val mainPage = "http://pluzz.francetv.fr/videos/c_dans_lair.html"
  
  def getMainPage: scala.concurrent.Future[String] = {
    client.url(mainPage).get.map(_.body)
  }
  
  val info = "http://webservices.francetelevisions.fr/tools/getInfosOeuvre/v2/?idDiffusion=%s&catalogue=Pluzz"
  
  
  def get(id: EpisodeId): scala.concurrent.Future[Episode] = {
    client.url(info.format(id.value)).get.map{r => Episode.epReads.reads(r.json).get}
  }
  
  def m3u(url: String): Future[Seq[String]] = {
    /**
     * The content of the file looks like this:
     * #EXTM3U
       #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=304000,RESOLUTION=320x180,CODECS="avc1.66.30, mp4a.40.2"
       http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/index_0_av.m3u8?null=
       #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=576000,RESOLUTION=512x288,CODECS="avc1.66.30, mp4a.40.2"
       http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/index_1_av.m3u8?null=
       #EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=832000,RESOLUTION=704x396,CODECS="avc1.77.30, mp4a.40.2"
       http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/index_2_av.m3u8?null=
     */
    client.url(url).get.map { resp => 
      resp.body.split("\n").filterNot(_.startsWith("#")).toList
    }
  }
  
  def close() = {
    client.close()
  }
  
}


trait StreamDownloader {
  
  def download(url: URL, dest: File): Future[File]
  
}

class VLC(path: String) extends StreamDownloader {
  // /Applications/VLC.app/Contents/MacOS/VLC -I dummy
  // -vvv http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/master.m3u8 
  // --sout file/ts:test vlc://quit
  
  override def download(streamUrl: URL, dest: File): Future[File] = {
    println(s"now downloading $streamUrl")
    val filename = "test-video"
    val command = Seq(path, "-I", "dummy", "-vvv", streamUrl.toString, "--sout", s"file/ts:$filename", "vlc://quit")
    Future(Process(command).run).map(_ => dest)
  }
  
}


object Downloading {
  
//  def download(streamUrl: String, dest: File): Future[File] = {
//    println(s"now downloading $streamUrl")
//    Future.successful(dest)
//  }
//  
//  def download(streaming: String, dest: File): Either[String, File] = {
//    val rightFormat = ep.videos.find(_.format == format).toRight(s"Could not find a video with the format $format")
//
//    rightFormat.right.map { v =>
//      println(s"now downloading ${v.url}")
//      new File("")
//    }
//  }
  
}

object Parsing {
  
  def extractIds(html: String): Seq[EpisodeId] = {
    // <a class="video" id="current_video" href="http://info.francetelevisions.fr/?id-video=121434851" style="display: none;">Voir la vidéo</a>
    //<a href="/videos/c_dans_lair_,121434783.html" class="row"><div class="autre-emission-c4">Emission du 29-04 à 17:46</div><div class="autre-emission-c3">En replay</div></a>
    val latestPattern = """href="http://info.francetelevisions.fr/\?id-video=(\d+)"""".r
    val olderPattern = """href="/videos/c_dans_lair_,(\d+).html"""".r
    
    val latest = latestPattern.findFirstMatchIn(html).map(_.group(1)).get // there has to be a latest!
    val olderOnes = olderPattern.findAllMatchIn(html).map(_.group(1)).toList
    (olderOnes :+ latest).map(s => EpisodeId(s))
  }
  
}
