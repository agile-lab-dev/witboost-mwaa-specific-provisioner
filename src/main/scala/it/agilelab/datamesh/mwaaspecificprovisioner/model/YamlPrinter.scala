package it.agilelab.datamesh.mwaaspecificprovisioner.model

import io.circe.Json

trait YamlPrinter {
  def printAsYaml(json: Json): String = io.circe.yaml.Printer.spaces2.copy(preserveOrder = true).pretty(json)
}
