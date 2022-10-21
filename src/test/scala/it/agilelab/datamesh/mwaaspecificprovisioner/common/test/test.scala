package it.agilelab.datamesh.mwaaspecificprovisioner.common

import scala.io.Source
import scala.util.Using

package object test {

  def getTestResourceAsString(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    Using(Source.createBufferedSource(stream))(source => source.getLines().mkString("\n")).get
  }
}
