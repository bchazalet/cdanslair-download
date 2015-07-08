package com.bchazalet.cdanslair

case class Episode(id: EpisodeId, sous_titre: String, diffusion: Diffusion, videos: Seq[Video])

object Episode {

  /** whether the file is already present (i.e. downloaded on the file system) */
  def isPresent(ep: Episode, filenames: Seq[String]): Boolean = find(ep, filenames).isDefined

  def find(ep: Episode, filenames: Seq[String]): Option[String] = filenames.find(f => isPresent(ep, f))

  /** whether the filename corresponds to the episode */
  def isPresent(ep: Episode, filename: String): Boolean = filename.startsWith(ep.id.value)

}
