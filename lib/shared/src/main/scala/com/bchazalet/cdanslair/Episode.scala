package com.bchazalet.cdanslair

case class Episode(id: EpisodeId, sous_titre: String, diffusion: Diffusion, videos: Seq[Video])

object Episode {

  /** whether the file is already present (i.e. downloaded on the file system) */
  def isPresent(filenames: Seq[String], ep: Episode): Boolean = filenames.find(_.startsWith(ep.id.value)).isDefined

}
