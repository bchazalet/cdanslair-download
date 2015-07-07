package com.bchazalet.cdanslair

import scala.util.Try
import upickle.Js

class EpisodeId private(val value: String) extends AnyVal {

  override def toString() = value

}

object EpisodeId {

  val valid = """^\d+$""".r

  def apply(value: String): EpisodeId = {
    require(EpisodeId.valid.findFirstIn(value).isDefined, s"$value does not look like a valid episode id")
    new EpisodeId(value)
  }

  implicit val epIdReader = upickle.Reader[EpisodeId]{
    case Js.Str(str) => EpisodeId(str)
  }

}
