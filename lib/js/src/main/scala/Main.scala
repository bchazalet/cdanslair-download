import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.jquery.jQuery
import js.Dynamic.{global => g}
import js.DynamicImplicits._
import org.scalajs.dom
import dom.document
import com.bchazalet.cdanslair._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object MainApp extends js.JSApp {

  val fs = g.require("fs")
  // val path = g.require("path")

  def main(): Unit = {
    val folder = "/Users/bchazalet/Downloads/pluzz/cdanslair"
    val filenames = listFiles(folder)

    val client = new com.bchazalet.cdanslair.XhrCdanslairClient()

    val downloader = new VlcDownloader(VLC.defaultPath)

    val all = client.fetch

    all.map(eps => show(eps, filenames))

    all.map { eps =>
      val ep = eps.head
      val video = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get
      val download = downloader.download(video.url, folder + "/" + eps.head.id.value + "-test.ts")
      import scala.scalajs.js.timers._
      setTimeout(5000) {
        println("cancelling download")
        download.cancel
      }
    }

  }

  def show(eps: Seq[Episode], filenames: Seq[String]) = {
    jQuery("body").append("<ul>")

    eps.foreach{ (ep: Episode) =>
      val status = if(Episode.isPresent(ep, filenames)) "Already downloaded" else "Available for download"
      jQuery("body").append("<li>" + ep.sous_titre + " ~ " + status + "</li>")
    }
    jQuery("body").append("</ul>")
  }

  def display(filenames: Seq[String]) = {
    jQuery("body").append("<ul>")
    filenames.foreach{ (filename: String) =>
        jQuery("body").append("<li>" + filename + "</li>")
    }
    jQuery("body").append("</ul>")
  }

  def listFiles(path: String): Seq[String] = {
    fs.readdirSync(path).asInstanceOf[js.Array[String]]
  }

  // def download(url: URL, dest: String): Unit = {
  //   val spawn = g.require("child_process").spawn
  //   val cmd = "/Applications/VLC.app/Contents/MacOS/VLC"
  //   val args = js.Array("-I", "dummy", "-vvv", url, "--sout", s"file/ts:" + dest, "vlc://quit")
  //
  //   println("starting the download")
  //   println(cmd)
  //
  //   val child = spawn(cmd, args, js.Dynamic.literal(stdio = "ignore"))
  //
  //   child.on("close", { (code: Int) =>
  //     println("process exit code " + code)
  //   })
  //
  //   println("download started")
  // }

}
