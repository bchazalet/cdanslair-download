package com.bchazalet.cdanslair

import java.net.URL
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.Json
import java.io.File
import org.joda.time.format.DateTimeFormat
import scala.concurrent.ExecutionContext

object DownloadApp extends App {
  
  val defaultVlc = "/Applications/VLC.app/Contents/MacOS/VLC"
  
  val appVersion = "1.1"
  val appName = "cdanslair-download"
  
  case class Config(out: File = new File("."), vlcPath: File = new File(defaultVlc))
  val parser = new scopt.OptionParser[Config](appName) {
    head(appName, appVersion)
    opt[File]('o', "out") required() valueName("<folder>") action { (x, c) =>
      c.copy(out = x) } text("the output folder where to download the video files.")
    
    opt[File]("vlc") valueName("<file>") action { (x, c) =>
      c.copy(vlcPath = x) } text(s"the path to your vlc program. Default points to $defaultVlc")
    
  }
  
  parser.parse(args, Config()) match {
    case Some(c) => run(c)
    case None => // arguments are bad, error message will have been displayed
  }

  def run(config: Config) = {
    
    val outputFolder = config.out
    
    val format = DateTimeFormat.forPattern("dd-MM-YYYY")
    
    val client = new CdanslairClient()
    
    val episodesF = client.fetch()
    
    val undownloadedF = episodesF.map { episodes =>
      val eps = episodes.sortWith(_ > _) // most recent first
      val files = outputFolder.listFiles
      def isPresent(ep: Episode): Boolean = files.find(_.getName.startsWith(ep.id.value)).isDefined
      val todo = eps.filter(!isPresent(_))
      println(s"There are ${todo.size} episodes to download:")
      todo.foreach(ep => println(s"${ep.diffusion.publishedAt.toString(format)} -> ${ep.id} - ${ep.sous_titre}"))
      todo
    }
    
    val streamDownloader: StreamDownloader = new VLC(config.vlcPath)
    val eofStream: CancelEventStream = new ConsoleEOFEventStream()
    
    try {
    
      val undownloaded = Await.result(undownloadedF, 1 minute)
    
      undownloaded.foreach { ep =>
        val rightFormat = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get //.toRight(s"Could not find a video with the format ${Format.M3U8_DOWNLOAD}")
        println(s"Downloading episode: ${ep.id} - ${ep.sous_titre}")
        println(s"Press Ctrl+D to cancel this download only")
        val current = streamDownloader.download(new URL(rightFormat.url), new File(outputFolder, s"${ep.id.value}-${ep.diffusion.publishedAt.toString(format)}.ts"))
        val eof = eofStream.next
        val both = Future.firstCompletedOf(Seq(eof, current.future))
        Await.ready(both, 2 hours)
        if(eof.isCompleted){
          // if we've completed because of EOF input from user, than cancel the download
          current.cancel
          println("::Cancelled")
        }
      }
      
      println("all done, bye now!")
    
    } catch {
      case e: Exception =>  println("Error: " + e.getMessage)
    } finally {
      client.close()
      eofStream.stop()
    }
  
  }
  
}

