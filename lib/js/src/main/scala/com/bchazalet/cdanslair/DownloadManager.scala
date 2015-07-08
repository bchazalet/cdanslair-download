package com.bchazalet.cdanslair

import scala.scalajs.js
import scala.collection.mutable.Queue
import js.Dynamic.{global => g}

class DownloadManager(downloader: StreamDownloader, destFolder: String) {
  val path = g.require("path")

  var queue = Queue.empty[Episode]

  var current = Option.empty[(Episode, StreamDownload)]

  var handlers = Seq.empty[DownloadStatus.Handler]

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

  def register(handler: DownloadStatus.Handler): Unit = {
    handlers = handlers :+ handler
    sendStatus()
  }

  private def sendStatus() = {
    val s = status()
    handlers.foreach(h => h(s))
  }

  private def status(): DownloadStatus = {
    current.map { case (ep, download) => Downloading(ep, 0) }.getOrElse(Idle)
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
    sendStatus()
  }

}
