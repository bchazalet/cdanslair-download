import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.jquery.jQuery
import js.Dynamic.{global => g}
import js.DynamicImplicits._
import org.scalajs.dom
import dom.document
import com.bchazalet.cdanslair.Episode
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

object MainApp extends js.JSApp {

  val fs = g.require("fs")
  // val path = g.require("path")

  def main(): Unit = {
    val filenames = listFiles("/Users/bchazalet/Downloads/pluzz/cdanslair")

    // display(filenames)

    val client = new com.bchazalet.cdanslair.XhrCdanslairClient()

    client.fetch.map(eps => show(eps))

    // println("a test in console")
  }

  def show(eps: Seq[Episode]) = {
    jQuery("body").append("<ul>")
    eps.foreach{ (ep: Episode) =>
        jQuery("body").append("<li>" + ep.id + " -> " + ep.sous_titre + "</li>")
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

}
