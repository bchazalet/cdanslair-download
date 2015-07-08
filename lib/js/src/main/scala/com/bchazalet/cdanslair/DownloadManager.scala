package com.bchazalet.cdanslair

import scala.scalajs.js
import scala.collection.mutable.Queue
import js.Dynamic.{global => g}
import scala.scalajs.js.timers._
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

class DownloadManager(downloader: StreamDownloader, destFolder: String) {
  val path = g.require("path")

  var queue = Queue.empty[Episode]

  var current = Option.empty[(Episode, String, StreamDownload)]

  var handlers = Seq.empty[DownloadStatus.Handler]

  // we regularly want to update our status
  setInterval(1000) {
    sendStatus()
  }

  /** add to the queue of episode to be downloaded */
  def add(ep: Episode) = {
    queue += ep
    start()
  }

  /** cancel current */
  def cancel(): Unit = {
    current.foreach { case (ep, dest, download) => download.cancel }
    current = None
    start() // start the next one
  }

  def register(handler: DownloadStatus.Handler): Unit = {
    handlers = handlers :+ handler
    Future(sendStatus())
  }

  private def sendStatus() = {
    val s = status()
    handlers.foreach(h => h(s))
  }

  private def status(): DownloadStatus = {
    current.map { case (ep, dest, download) =>
      val size = FileUtils.filesize(dest)
      Downloading(ep, size)
    }.getOrElse(Idle)
  }

  private def start() = {
    if(!queue.isEmpty && current.isEmpty){
      val ep = queue.dequeue
      // TODO handle the case where the right format is not available
      val video = ep.videos.find(_.format == Format.M3U8_DOWNLOAD).get
      val dest = path.join(destFolder, filename(ep)).asInstanceOf[String]
      val download = downloader.download(video.url, dest)
      current = Option(ep, dest, download)
    }
    Future(sendStatus())
  }

  private def filename(ep: Episode) = {
    s"${ep.id.value}-${formatDate(ep.diffusion.startDate)}.ts"
  }

  // TODO we can go better. Use js.Date
  private def formatDate(startDate: String): String = {
    // from 07/07/2015 17:49 to 07-07-2015
    startDate.trim.take(10).replace("/", "-")
  }

}
