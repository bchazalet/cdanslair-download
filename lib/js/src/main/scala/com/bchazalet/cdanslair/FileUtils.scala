package com.bchazalet.cdanslair

import scala.scalajs.js
import js.Dynamic.{global => g}

object FileUtils {
  val fs = g.require("fs")

  val KB = 1024
  val MB = KB*KB

  def listFiles(path: String): Seq[String] = {
    fs.readdirSync(path).asInstanceOf[js.Array[String]]
  }

  /** filesize in MB */
  def filesize(filePath: String): Int = {
    fs.statSync(filePath).size.asInstanceOf[Int] / MB
  }

}
