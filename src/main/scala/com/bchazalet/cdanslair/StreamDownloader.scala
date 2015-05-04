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
  
  /** blocks until done */
  def download(url: URL, dest: File): File
  
}

class VLC(path: File) extends StreamDownloader {
  // /Applications/VLC.app/Contents/MacOS/VLC -I dummy
  // -vvv http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/master.m3u8 
  // --sout file/ts:test vlc://quit
  
  override def download(streamUrl: URL, dest: File): File = {
    val command = Seq(path.getAbsolutePath, "-I", "dummy", "-vvv", streamUrl.toString, "--sout", s"file/ts:${dest.getAbsolutePath}", "vlc://quit")
    val vlcLogger = ProcessLogger(_ => (), _ => ())
    Process(command).!!(vlcLogger)
    dest
  }
  
}

object Dummy extends StreamDownloader {
  
  def download(url: URL, dest: File): File = dest
  
}