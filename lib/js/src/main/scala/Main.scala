import scala.scalajs.js
import js.Dynamic.{global => g}
import js.annotation.JSExport
import org.scalajs.jquery.jQuery
import js.DynamicImplicits._
import org.scalajs.dom
import dom.document
import com.bchazalet.cdanslair._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object MainApp extends js.JSApp {

  val fs = g.require("fs")
  val path = g.require("path")

  val folder = "/Users/bchazalet/Downloads/pluzz/cdanslair-tmp"

  val client = new com.bchazalet.cdanslair.XhrCdanslairClient()
  val manager = new DownloadManager(new VlcDownloader(VLC.defaultPath), folder)

  var autostart = true

  def main(): Unit = {

    // register for status update
    manager.register(update)

    val filenames = FileUtils.listFiles(folder)

    val all = client.fetch

    all.map(eps => show(eps, filenames))

    if(autostart){
      // queue all available for download
      all.map { _.filter(ep => !Episode.isPresent(ep, filenames)).foreach { ep =>
        println(s"adding ${ep.id} for download")
        manager.add(ep)
      }}
    }
  }

  @JSExport
  def cancel() = manager.cancel()

  def show(eps: Seq[Episode], filenames: Seq[String]) = {
    jQuery("body").append("<ul>")

    eps.foreach{ (ep: Episode) =>
      val fileOnDisk = Episode.find(ep, filenames).map(f => path.join(folder, f).asInstanceOf[String])
      val status = fileOnDisk.map(f => s"Already downloaded (${FileUtils.filesize(f)}MB)").getOrElse("Available for download")
      jQuery("#content").append(s"<li>${ep.diffusion.startDate} - ${ep.sous_titre} ~ $status</li>")
    }
    jQuery("#content").append("</ul>")
  }

  val AVERAGE_SIZE = 470 // MB

  def update(status: DownloadStatus): Unit = {
    val msg = status match {
      case Idle => "Nothing is being downloaded"
      case Downloading(ep, progress) => s"Downloading ${ep.diffusion.startDate} - ${ep.sous_titre} - ${progress}MB (or ~${percent(progress)}%)"
    }

    // FIXME does not seem to work the first time
    jQuery("#status").text(s"$msg")
  }

  private def percent(progress: Int): Int = {
    scala.math.round(progress.toFloat * 100f/AVERAGE_SIZE.toFloat)
  }

  def display(filenames: Seq[String]) = {
    jQuery("body").append("<ul>")
    filenames.foreach{ (filename: String) =>
        jQuery("body").append("<li>" + filename + "</li>")
    }
    jQuery("body").append("</ul>")
  }



}
