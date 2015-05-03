package com.bchazalet.cdanslair

import java.net.URL
import java.io.File
import scala.concurrent.Future
import scala.sys.process.Process
import scala.concurrent.ExecutionContext

/**
 * Trait to abstract the different implementations of a stream downloader (be it using vlc, mplayer or even natively in java)
 */
trait StreamDownloader {
  
  def download(url: URL, dest: File): Future[File]
  
}

class VLC(path: String)(implicit ec: ExecutionContext) extends StreamDownloader {
  // /Applications/VLC.app/Contents/MacOS/VLC -I dummy
  // -vvv http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/master.m3u8 
  // --sout file/ts:test vlc://quit
  
  override def download(streamUrl: URL, dest: File): Future[File] = {
    println(s"now downloading $streamUrl")
//    val filename = "test-video"
    val command = Seq(path, "-I", "dummy", "-vvv", streamUrl.toString, "--sout", s"file/ts:${dest.getAbsolutePath}", "vlc://quit")
    Future(Process(command).run).map(_ => dest)
  }
  
}