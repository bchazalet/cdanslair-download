package com.bchazalet.cdanslair

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object Episodes {
  import scala.language.implicitConversions

  implicit class EpisodeFormattedDate(val ep: Episode) extends AnyVal {

    // "date_debut":"01\/05\/2015 17:50"
    def startedAt: DateTime = {
      DateTime.parse(ep.diffusion.startDate, DateTimeFormat.forPattern("dd/MM/YYYY HH:mm"))
    }

  }

  object NewestToOldest extends Ordering[Episode] {
    override def compare(ep1: Episode, ep2: Episode) = ep1.startedAt.compareTo(ep2.startedAt)
  }

}
