package com.bchazalet.cdanslair

import org.scalatest.FunSuite
import scala.io.Source

class ParsingTests extends FunSuite {

  test("test that we can extract episodes' ids from the html"){
    val html = Source.fromFile(getClass.getResource("/c_dans_lair.html").toURI).mkString
    val result = Parsing.extractIds(html)
    assert(result.size == 5)
    println(result)
  }
  
}