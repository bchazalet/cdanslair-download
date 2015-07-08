package com.bchazalet.cdanslair

import java.io.File
import scala.scalajs.js
import scala.concurrent._
import js.Dynamic.{global => g}
import js.DynamicImplicits._
import js.JSConverters._

class ChildProcessDownload(child: js.Dynamic, dest: String)(implicit ec: ExecutionContext) extends StreamDownload {

  override val future = {
    val promise = Promise.apply[String]()

    child.on("close", { (code: Int) =>
      println("process exit code " + code)
      promise.success(dest)
    })

    promise.future
  }

  override def cancel = child.kill("SIGKILL")

}

class VlcDownloader(path: String)(implicit ec: ExecutionContext) extends StreamDownloader {

  override def download(url: String, dest: String) = {
    val spawn = g.require("child_process").spawn

    val args: js.Array[String] = VLC.args(url, dest).toJSArray

    println("starting the download")

    val child = spawn(path, args, js.Dynamic.literal(stdio = "ignore"))

    println("download started")

    new ChildProcessDownload(child, dest)

  }

}
