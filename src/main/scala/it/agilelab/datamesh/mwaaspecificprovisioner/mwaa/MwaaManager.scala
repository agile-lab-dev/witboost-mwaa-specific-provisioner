package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import cats.data.ValidatedNel
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import it.agilelab.datamesh.mwaaspecificprovisioner.common.StringUtils.StringImplicits
import it.agilelab.datamesh.mwaaspecificprovisioner.error.{ErrorType, ProvisionErrorType, ValidationErrorType}
import it.agilelab.datamesh.mwaaspecificprovisioner.model.MwaaFields
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3Gateway
import it.agilelab.datamesh.mwaaspecificprovisioner.validation.Validator

class MwaaManager(s3Client: S3Gateway, mwaaValidator: Validator) extends LazyLogging {

  def executeValidation(descriptor: String): ValidatedNel[ValidationErrorType, MwaaFields] = mwaaValidator
    .validate(descriptor)

  def executeProvision(descriptor: String): ValidatedNel[ErrorType, Unit] = mwaaValidator.validate(descriptor)
    .andThen { mwaaFields =>
      s3Client.copyObject(
        mwaaFields.bucketName,
        s"${mwaaFields.destinationPath.ensureTrailingSlash}${mwaaFields.prefix}.${mwaaFields.dagName}",
        mwaaFields.bucketName,
        s"${mwaaFields.sourcePath.ensureTrailingSlash}${mwaaFields.prefix}.${mwaaFields.dagName}"
      ).leftMap { e =>
        logger.error(s"Error in executeProvision: ${e.show}")
        ProvisionErrorType(e)
      }.toValidatedNel
    }

  def executeUnprovision(descriptor: String): ValidatedNel[ErrorType, Unit] = mwaaValidator.validate(descriptor)
    .andThen { mwaaFields =>
      s3Client.deleteObject(
        mwaaFields.bucketName,
        s"${mwaaFields.destinationPath.ensureTrailingSlash}${mwaaFields.prefix}.${mwaaFields.dagName}"
      ).leftMap { e =>
        logger.error(s"Error in executeUnprovision: ${e.show}")
        ProvisionErrorType(e)
      }.toValidatedNel
    }

}
