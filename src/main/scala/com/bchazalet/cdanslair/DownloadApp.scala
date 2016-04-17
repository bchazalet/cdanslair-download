package com.bchazalet.cdanslair

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{ Try, Success, Failure }
import scala.util.control.NonFatal
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import com.bchazalet.cdanslair.CancelEventStream._
import play.api.libs.json.Json
import java.net.URL
import java.io.File

object DownloadApp extends App {

  val defaultVlc = "/Applications/VLC.app/Contents/MacOS/VLC"

  val appVersion = "1.2"
  val appName = "pluzz-download"

  case class Config(out: File = new File("."), vlcPath: File = new File(defaultVlc), replay: Replay = Replay.Cdanslair, proxy: Option[SocksProxy] = None)

  val replays = Map(
    "accuse" -> Replay.FaitesEntrerLaccuse,
    "cdanslair" -> Replay.Cdanslair
  )

  implicit val replayRead: scopt.Read[Replay] = scopt.Read.reads(replays.get(_).get)

  val parser = new scopt.OptionParser[Config](appName) {
    head(appName, appVersion)
    opt[File]('o', "out") required() valueName("<folder>") action { (x, c) =>
      c.copy(out = x) } text("the output folder where to download the video files.") validate { f =>
        if(f.exists && f.isDirectory) success else failure("output folder does not exit")
      }

    opt[File]("vlc") valueName("<file>") action { (x, c) =>
      c.copy(vlcPath = x) } text(s"the path to your vlc program. Default points to $defaultVlc")

    opt[String]('p', "proxy") valueName("socksProxy") action { (x, c) =>
      c.copy(proxy = SocksProxy.from(x)) } text("a socks proxy for the vlc to use when downloading the video") validate(s =>
        SocksProxy.from(s).map(_ => success).getOrElse(failure("proxy must have the following format host:port"))
      )

    opt[Replay]('r', "replay") valueName("accuse|cdanslair") action { (x, c) =>
      c.copy(replay = x)
    }

    opt[java.net.URI]('m', "main-page") valueName("<url>") action { (x, c) =>
      val attempt = Replay.generic(x.toString)
      c.copy(replay = attempt)
    } text("the url of the main page for a given replay that you'd try to download. This overrides the -r argument.")


  }

  parser.parse(args, Config()) match {
    case Some(c) => run(c).fold(error => print(CdlrError.forUser(error)), eps => println(s"all done (${eps.size} completed), bye now!"))
    case None => // arguments are bad, error message will have been displayed
  }

  /** returns the number of episodes *completed* */
  def run(config: Config): Either[CdlrError, Seq[Episode]] = {

    val outputFolder = config.out

    implicit val format = DateTimeFormat.forPattern("dd-MM-YYYY")

    val client = new PluzzClient(config.replay)

    val undownloadedF = client.fetch().map { episodes =>
      val eps = episodes.sortWith(_ > _) // most recent first
      val files = outputFolder.listFiles
      def isPresent(ep: Episode): Boolean = files.find(_.getName.startsWith(ep.id.value)).isDefined
      val todo = eps.filter(!isPresent(_))
      println(s"There are ${todo.size} episodes to download for replay '${config.replay.name}':")
      todo.foreach(ep => println(s"${ep.diffusion.publishedAt.toString(format)} -> ${ep.id} - ${ep.sous_titre}"))
      todo
    }.andThen { case _ => client.close() }

    val firstStep: Either[CdlrError, Seq[Episode]] =
      Try(Await.result(undownloadedF, 1 minute)) match {
        case Success(x) => Right(x)
        case Failure(ex) => Left(WebserviceError(None, Some(ex)))
      }

    firstStep.right.flatMap { undownloaded =>

      val sd: StreamDownloader = new VLC(config.vlcPath, config.proxy)
      tryDownload(undownloaded, sd, outputFolder)

    }

  }

  def forFilename(s: String): String = {
    val spaceRep = '_'
    s.replace(" ", spaceRep.toString).filter(c => c.isLetterOrDigit || c == spaceRep)
  }

  /** tries download the given list of episodes */
  def tryDownload(undownloaded: Seq[Episode], streamDownloader: StreamDownloader, outputFolder: File)(implicit dtf: DateTimeFormatter): Either[CdlrError, Seq[Episode]] = {
    val eofStream: CancelEventStream = new ConsoleEOFEventStream()

    try {
      val all = undownloaded.map { ep =>
        // we're ignoring -rather silently - the videos for which we don't find the right format
        ep.videos.find(_.format == Format.M3U8_DOWNLOAD).flatMap { rightFormat =>
          val sDesc = forFilename(ep.sous_titre)
          val dest = new File(outputFolder, s"${ep.id.value}-${ep.diffusion.publishedAt.toString(dtf)}-$sDesc.ts")
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
    val bestVideo = Await.result(bestDefinition(vid), 20 seconds)
    val current = downloader.download(new URL(bestVideo.url), dest)
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

  /** tries to auto-select the best definition (hack around the fact that now VLC can't pick up the best ones on its own)*/
  def bestDefinition(vid: Video): Future[Video] = {
    import com.ning.http.client.AsyncHttpClient
    if(vid.format == Format.M3U8_DOWNLOAD){
      // download the playlist file, and keep the last line, which should be the best definition
      val client = new AsyncHttpClient()
      PluzzClient.httpGet(vid.url)(client).map { r =>
        val lastLine = r.getResponseBody.split("\n").last
        vid.copy(url = lastLine)
      }
    } else {
      Future.successful(vid)
    }
  }
}
