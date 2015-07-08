package com.bchazalet.cdanslair

sealed trait DownloadStatus
case class Downloading(ep: Episode, progress: Int) extends DownloadStatus
object Idle extends DownloadStatus

object DownloadStatus {
  
  type Handler = DownloadStatus => Unit

}
