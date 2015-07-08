package com.bchazalet.cdanslair

import java.io.File
import scala.concurrent.Future
import scala.sys.process.{ProcessLogger, Process}
import scala.concurrent.ExecutionContext

class ProcessDownload(process: Process, dest: File)(implicit ec: ExecutionContext) extends StreamDownload {

  // TODO check exit value?
  override val future = Future { process.exitValue(); dest }

  override def cancel = process.destroy()

}

class VlcDownloader(path: File)(implicit ec: ExecutionContext) extends StreamDownloader {

  override def download(streamUrl: String, dest: File) = {
    val command = Seq(path.getAbsolutePath) ++ VLC.args(streamUrl, dest)
    val vlcLogger = ProcessLogger(_ => (), _ => ())
    new ProcessDownload(Process(command).run(vlcLogger), dest)
  }

}
