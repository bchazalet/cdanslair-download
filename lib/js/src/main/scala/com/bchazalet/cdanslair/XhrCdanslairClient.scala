package com.bchazalet.cdanslair

import org.scalajs.dom
import dom.ext._
import scala.concurrent.Future
import upickle._
import scala.concurrent.ExecutionContext

class XhrCdanslairClient(implicit ec: ExecutionContext) extends CdanslairClient {

  protected def getHomepage: Future[String] = Ajax.get(mainPage).map(_.responseText)

  /** fetches the currently published episodes */
  def fetch(): Future[Seq[Episode]] = {

    getHomepage flatMap { html =>
      val ids = extractIds(html)
      val all = ids.map(this.get(_))
      Future.sequence(all)
    }

  }

  def get(id: EpisodeId): scala.concurrent.Future[Episode] = {
    Ajax.get(info.format(id.value)).map{r => read[Episode](r.responseText)}
  }

  // def close() = {
  //   client.close()
  // }


}
