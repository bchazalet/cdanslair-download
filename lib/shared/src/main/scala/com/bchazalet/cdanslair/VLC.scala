package com.bchazalet.cdanslair

import java.io.File

object VLC {
  // /Applications/VLC.app/Contents/MacOS/VLC -I dummy
  // -vvv http://ftvodhdsecz-f.akamaihd.net/i/streaming-adaptatif/2015/S18/J4/121434817-20150430-,398k,632k,934k,.mp4.csmil/master.m3u8
  // --sout file/ts:test vlc://quit

  def args(url: String, dest: String) = Seq("-I", "dummy", "-vvv", url, "--sout", s"file/ts:$dest", "vlc://quit")

  val defaultPath = "/Applications/VLC.app/Contents/MacOS/VLC"

}
