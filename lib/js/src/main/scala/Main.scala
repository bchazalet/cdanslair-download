import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.jquery.jQuery
import js.Dynamic.{global => g}
import js.DynamicImplicits._
import org.scalajs.dom
import dom.document

object MainApp extends js.JSApp {

  def main(): Unit = {
    val fs = g.require("fs")
    val path = g.require("path")

    fs.readdir("/Users/bchazalet/Downloads", { (err: js.Dynamic, files: js.Array[String]) =>
        jQuery("body").append("<ul>")
        files.foreach{ (filename: String) =>
            jQuery("body").append("<li>" + filename + "</li>")
        }
        jQuery("body").append("</ul>")
    })
    println("a test in console")
  }

}
