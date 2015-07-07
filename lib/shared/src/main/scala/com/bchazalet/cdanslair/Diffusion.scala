package com.bchazalet.cdanslair

import upickle.key

case class Diffusion(timestamp: Int, @key("date_debut") startDate: String)
