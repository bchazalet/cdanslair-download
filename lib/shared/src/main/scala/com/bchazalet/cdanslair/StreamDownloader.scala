package com.bchazalet.cdanslair

import java.io.File
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Trait to abstract the different implementations of a stream downloader (be it using vlc, mplayer or even natively in java)
 */
trait StreamDownloader {

  /** starts a download a return a handle to retrieve future and for cancellation */
  def download(url: String, dest: String): StreamDownload

}

trait StreamDownload {

  /** the future that will be completed when the download is completed */
  def future: Future[String]

  def cancel: Unit

}

/** for testing only */
object Dummy extends StreamDownloader {

  override def download(url: String, dest: String) = new StreamDownload {

    override val future = Future.successful(dest)

    def cancel = ()
  }

}
