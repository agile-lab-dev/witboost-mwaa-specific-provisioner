package it.agilelab.datamesh.mwaaspecificprovisioner.validation

import cats.data.ValidatedNel
import it.agilelab.datamesh.mwaaspecificprovisioner.error.ValidationErrorType
import it.agilelab.datamesh.mwaaspecificprovisioner.model.MwaaFields

trait Validator {
  def validate(descriptor: String): ValidatedNel[ValidationErrorType, MwaaFields]
}
