package com.bchazalet.cdanslair

import scala.scalajs.js
import scala.collection.mutable.Queue
import js.Dynamic.{global => g}

class DownloadManager(downloader: StreamDownloader, destFolder: String) {
  val path = g.require("path")

  var queue = Queue.empty[Episode]

  var current = Option.empty[(Episode, StreamDownload)]

  /** add to the queue of episode to be downloaded */
  def add(ep: Episode) = {
    queue += ep
    start()
  }

  /** cancel current */
  def cancel(): Unit = {
    current.foreach { case (ep, download) => download.cancel }
    current = None
    start() // start the next one
  }

  private def start() = {
    if(!queue.isEmpty && current.isEmpty){
      val ep = queue.dequeue
      // TODO handle the case where the right format is not available
      val video = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get
      val dest = path.join(destFolder, ep.id.value + "-test.ts").asInstanceOf[String]
      val download = downloader.download(video.url, dest)
      current = Option(ep, download)
    }
  }

}
