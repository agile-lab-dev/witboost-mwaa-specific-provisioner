package it.agilelab.datamesh.specificprovisioner.mwaa

import io.circe.generic.extras.{Configuration, ConfiguredJsonCodec}

@ConfiguredJsonCodec
case class DagModel(isPaused: Boolean)

object DagModel {
  implicit val customConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
