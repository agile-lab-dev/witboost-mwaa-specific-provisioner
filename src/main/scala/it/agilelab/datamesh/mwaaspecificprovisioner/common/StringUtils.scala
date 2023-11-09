package it.agilelab.datamesh.mwaaspecificprovisioner.common

object StringUtils {

  implicit class StringImplicits(val s: String) {
    def ensureTrailingSlash: String = if (s.endsWith("/")) s else s"$s/"
  }
}
