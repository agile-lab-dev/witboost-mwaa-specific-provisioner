package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import cats.data.ValidatedNel
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.yaml.{parser, Printer}
import it.agilelab.datamesh.mwaaspecificprovisioner.common.Constants
import it.agilelab.datamesh.mwaaspecificprovisioner.common.StringUtils.StringImplicits
import it.agilelab.datamesh.mwaaspecificprovisioner.error.{
  ErrorType,
  GenericErrorType,
  ProvisionErrorType,
  ValidationErrorType
}
import it.agilelab.datamesh.mwaaspecificprovisioner.model.MwaaFields
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3Gateway
import it.agilelab.datamesh.mwaaspecificprovisioner.validation.Validator

import java.io.{BufferedWriter, File, FileWriter}
import scala.util.{Failure, Success, Try}

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
      }.toValidatedNel.andThen { _ =>
        Try {
          val tempFile = createTmpDescriptorFile(mwaaFields, ".yaml")
          try {
            writeTmpDescriptorFile(descriptor, tempFile)
            s3Client.createFile(
              mwaaFields.bucketName,
              s"${mwaaFields.destinationPath.ensureTrailingSlash}${mwaaFields.prefix}_descriptor.yaml",
              tempFile
            ).leftMap { e =>
              logger.error(s"Error in executeProvision during descriptor upload: ${e.show}")
              ProvisionErrorType(e)
            }.toValidatedNel.andThen(_ => ().validNel)
          } finally {
            if (tempFile.exists()) tempFile.delete()
            ()
          }
        } match {
          case Success(validatedNel) => validatedNel
          case Failure(exception)    =>
            logger.error(s"Exception in executeProvision: ${exception.getMessage}")
            GenericErrorType(exception.getMessage).invalidNel
        }
      }
    }

  def createTmpDescriptorFile(mwaaFields: MwaaFields, extension: String) = File
    .createTempFile(s"${mwaaFields.prefix}_descriptor", extension)

  def writeTmpDescriptorFile(descriptor: String, tempFile: File) = {
    val descriptorJson  = parser.parse(descriptor).getOrElse(Json.Null)
    val dataProductJson = descriptorJson.hcursor.downField(Constants.DATA_PRODUCT_FIELD).focus.getOrElse(Json.Null)
    val dataProductYaml = Printer.spaces2.copy(preserveOrder = true).pretty(dataProductJson)
    val fileWriter      = new FileWriter(tempFile)
    val bufferedWriter  = new BufferedWriter(fileWriter)
    bufferedWriter.write(dataProductYaml)
    bufferedWriter.close()
    fileWriter.close()
  }

  def executeUnprovision(descriptor: String): ValidatedNel[ErrorType, Unit] = mwaaValidator.validate(descriptor)
    .andThen { mwaaFields =>
      s3Client.deleteObject(
        mwaaFields.bucketName,
        s"${mwaaFields.destinationPath.ensureTrailingSlash}${mwaaFields.prefix}.${mwaaFields.dagName}"
      ).leftMap { e =>
        logger.error(s"Error in executeUnprovision: ${e.show}")
        ProvisionErrorType(e)
      }.toValidatedNel.andThen { _ =>
        s3Client.deleteObject(
          mwaaFields.bucketName,
          s"${mwaaFields.destinationPath.ensureTrailingSlash}${mwaaFields.prefix}_descriptor.yaml"
        ).leftMap { e =>
          logger.error(s"Error in executeUnprovision during descriptor deleting: ${e.show}")
          ProvisionErrorType(e)
        }.toValidatedNel
      }
    }

}
