package it.agilelab.datamesh.specificprovisioner.model

import io.circe.Json

trait YamlPrinter {
  def printAsYaml(json: Json): String = io.circe.yaml.Printer.spaces2.copy(preserveOrder = true).pretty(json)
}
