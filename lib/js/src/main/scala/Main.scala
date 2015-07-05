import scala.scalajs.js
import js.annotation.JSExport
import org.scalajs.dom

object MainApp extends js.JSApp {

  def main(): Unit = {
    dom.document.write("just a quick test")
    println("a test in console")
  }

}
