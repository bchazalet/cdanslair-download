package com.bchazalet.cdanslair

sealed trait CdlrError
case class WebserviceError(msg: Option[String], ex: Option[Throwable]) extends CdlrError
case class DownloadError(msg: Option[String], ex: Option[Throwable]) extends CdlrError
//case object WrongFormat() extends CdlrError
case class Unknown(ex: Option[Throwable]) extends CdlrError

object CdlrError {
  
  def forUser(err: CdlrError): String = err match {
    case WebserviceError(msg, _) => s"we couldn't connect to the cdanslair servers ${msg.map("(" + _ + ")").getOrElse("")}"
    case DownloadError(msg, _) => s"we had an error while downloading and saving the video stream ${msg.map("(" + _ + ")").getOrElse("")}"
    case Unknown(ex) => s"we encountered an unexpected error ${ex.map("(" + _.getMessage + ")").getOrElse("")}"
  } 
  
}