package com.bchazalet.cdanslair

import org.scalajs.dom
import dom.ext._
import scala.concurrent.Future
import upickle._
import scala.concurrent.ExecutionContext

class XhrCdanslairClient(implicit val ec: ExecutionContext) extends CdanslairClient {

  override def getHomepage() = Ajax.get(mainPage).map(_.responseText)

  override def get(id: EpisodeId) = {
    Ajax.get(info.format(id.value)).map{r => read[Episode](r.responseText)}
  }

}
