package com.bchazalet.cdanslair

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{ Try, Success, Failure }
import scala.util.control.NonFatal
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import com.bchazalet.cdanslair.CancelEventStream._
import java.net.URL
import java.io.File

object DownloadApp extends App {
  import Episodes.EpisodeFormattedDate // for implicit date conversion

  val defaultVlc = "/Applications/VLC.app/Contents/MacOS/VLC"

  val appVersion = "1.1"
  val appName = "cdanslair-download"

  case class Config(out: File = new File("."), vlcPath: File = new File(defaultVlc))

  val parser = new scopt.OptionParser[Config](appName) {
    head(appName, appVersion)
    opt[File]('o', "out") required() valueName("<folder>") action { (x, c) =>
      c.copy(out = x) } text("the output folder where to download the video files.") validate { f =>
        if(f.exists && f.isDirectory) success else failure("output folder does not exit")
      }

    opt[File]("vlc") valueName("<file>") action { (x, c) =>
      c.copy(vlcPath = x) } text(s"the path to your vlc program. Default points to $defaultVlc")
  }

  parser.parse(args, Config()) match {
    case Some(c) => run(c).fold(error => print(CdlrError.forUser(error)), eps => println(s"all done (${eps.size} completed), bye now!"))
    case None => // arguments are bad, error message will have been displayed
  }

  /** returns the number of episodes *completed* */
  def run(config: Config): Either[CdlrError, Seq[Episode]] = {

    val outputFolder = config.out

    implicit val format = DateTimeFormat.forPattern("dd-MM-YYYY")

    val client = new CdanslairClient()

    val undownloadedF = client.fetch().map { episodes =>
      val eps = episodes.sorted(Episodes.NewestToOldest) // most recent first
      val files = outputFolder.listFiles
      def isPresent(ep: Episode): Boolean = files.find(_.getName.startsWith(ep.id.value)).isDefined
      val todo = eps.filter(!isPresent(_))
      println(s"There are ${todo.size} episodes to download:")
      todo.foreach(ep => println(s"${ep.startedAt.toString(format)} -> ${ep.id} - ${ep.sous_titre}"))
      todo
    }.andThen { case _ => client.close() }

    val firstStep: Either[CdlrError, Seq[Episode]] =
      Try(Await.result(undownloadedF, 1 minute)) match {
        case Success(x) => Right(x)
        case Failure(ex) => Left(WebserviceError(Option(ex.getMessage), Option(ex)))
      }

    firstStep.right.flatMap { undownloaded =>

      val sd: StreamDownloader = new VLC(config.vlcPath)
      tryDownload(undownloaded, sd, outputFolder)

    }

  }

  /** tries download the given list of episodes */
  def tryDownload(undownloaded: Seq[Episode], streamDownloader: StreamDownloader, outputFolder: File)(implicit dtf: DateTimeFormatter): Either[CdlrError, Seq[Episode]] = {
    val eofStream: CancelEventStream = new ConsoleEOFEventStream()

    try {
      val all = undownloaded.map { ep =>
        // we're ignoring -rather silently - the videos for which we don't find the right format
        ep.videos.find(_.format == Format.M3U8_DOWNLOAD).flatMap { rightFormat =>
          val dest = new File(outputFolder, s"${ep.id.value}-${ep.startedAt.toString(dtf)}.ts")
          println(s"Downloading episode: ${ep.id} - ${ep.sous_titre}")
          println(s"Press Ctrl+D to cancel this download only")
          if(download(rightFormat, dest, streamDownloader, eofStream.next))
            Some(ep)
          else None
        }
      }

      Right(all.flatten)
    } catch {
      case NonFatal(ex) => Left(DownloadError(Some("we couldn't download the video stream"), Some(ex)))
    } finally {
      eofStream.stop()
    }
  }

  /** downloads the video stream and blocks until the stream is completed or the cancel event */
  def download(vid: Video, dest: File, downloader: StreamDownloader, cancel: Future[CancelEvent])(implicit ec: ExecutionContext): Boolean = {
    val current = downloader.download(new URL(vid.url), dest)
    val both = Future.firstCompletedOf(Seq(cancel, current.future))
    Await.ready(both, 2 hours)
    if(cancel.isCompleted){
      // if we've completed because of EOF input from user, than cancel the download
      current.cancel
      println("::Cancelled")
      false
    } else {
      true
    }
  }
}
