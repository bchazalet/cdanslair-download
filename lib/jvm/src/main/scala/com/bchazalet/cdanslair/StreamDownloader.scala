package com.bchazalet.cdanslair

import java.net.URL
import java.io.File
import scala.concurrent.Future
import scala.sys.process.{ProcessLogger, Process}
import scala.concurrent.ExecutionContext

/**
 * Trait to abstract the different implementations of a stream downloader (be it using vlc, mplayer or even natively in java)
 */
trait StreamDownloader {
  
  /** starts a download a return a handle to retrieve future and for cancellation */
  def download(url: URL, dest: File): StreamDownload
  
}

trait StreamDownload {
  
  def future: Future[File]
  
  def cancel: Unit
  
}

class ProcessDownload(process: Process, dest: File)(implicit ec: ExecutionContext) extends StreamDownload {
  
  // TODO check exit value?
  override val future = Future { process.exitValue(); dest }
  
  override def cancel = process.destroy()
  
}

class VLC(path: File)(implicit ec: ExecutionContext) extends StreamDownloader {
  // /Applications/VLC.app/Contents/MacOS/VLC -I dummy
  // -vvv http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/master.m3u8 
  // --sout file/ts:test vlc://quit
  
  override def download(streamUrl: URL, dest: File) = {
    val command = Seq(path.getAbsolutePath, "-I", "dummy", "-vvv", streamUrl.toString, "--sout", s"file/ts:${dest.getAbsolutePath}", "vlc://quit")
    val vlcLogger = ProcessLogger(_ => (), _ => ())
    new ProcessDownload(Process(command).run(vlcLogger), dest)
  }
  
}

/** for testing only */
object Dummy extends StreamDownloader {
  
  def download(url: URL, dest: File) = new StreamDownload {
    
    override val future = Future.successful(dest) 
    
    def cancel = ()
  }
  
}