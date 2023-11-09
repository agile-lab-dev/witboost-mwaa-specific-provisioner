package it.agilelab.datamesh.mwaaspecificprovisioner.validation

import cats.data.{Validated, ValidatedNel}
import it.agilelab.datamesh.mwaaspecificprovisioner.error.{
  ErrorSourceFile,
  InvalidBucketName,
  InvalidComponent,
  InvalidComponentId,
  InvalidDagName,
  InvalidDescriptor,
  InvalidDestinationPath,
  InvalidScheduleCron,
  InvalidSourcePath,
  MissingSourceFile,
  ValidationErrorType
}
import it.agilelab.datamesh.mwaaspecificprovisioner.model.{
  ComponentDescriptor,
  MwaaFields,
  ProvisioningRequestDescriptor
}
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import it.agilelab.datamesh.mwaaspecificprovisioner.common.Constants
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3Gateway

class MwaaValidator(s3Gateway: S3Gateway) extends Validator with LazyLogging {

  private def validateComponent(
      provisioningRequestDescriptor: ProvisioningRequestDescriptor
  ): ValidatedNel[ValidationErrorType, ComponentDescriptor] = provisioningRequestDescriptor.getComponentToProvision
    .toRight(InvalidComponent(provisioningRequestDescriptor.componentIdToProvision)).toValidatedNel

  private def validateComponentId(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = {
    val urnArray = c.id.split(":")
    Validated.condNel(urnArray.length >= 6, s"${urnArray(3)}.${urnArray(4)}.${urnArray(5)}", InvalidComponentId(c.id))
  }

  private def validateDagName(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = c.specific.hcursor
    .downField(Constants.DAG_NAME_FIELD).as[String].leftMap { error =>
      logger.error("Error in validateDagName", error)
      InvalidDagName(Constants.DAG_NAME_FIELD, error)
    }.toValidatedNel

  private def validateDestinationPath(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = c.specific
    .hcursor.downField(Constants.DESTINATION_DAG_PATH_FIELD).as[String].leftMap { error =>
      logger.error("Error in validateDestinationPath", error)
      InvalidDestinationPath(Constants.DESTINATION_DAG_PATH_FIELD, error)
    }.toValidatedNel

  private def validateSourcePath(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = c.specific.hcursor
    .downField(Constants.SOURCE_DAG_PATH_FIELD).as[String].leftMap { error =>
      logger.error("Error in validateSourcePath", error)
      InvalidSourcePath(Constants.SOURCE_DAG_PATH_FIELD, error)
    }.toValidatedNel

  private def validateBucketName(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = c.specific.hcursor
    .downField(Constants.BUCKET_NAME_FIELD).as[String].leftMap { error =>
      logger.error("Error in validateBucketName", error)
      InvalidBucketName(Constants.BUCKET_NAME_FIELD, error)
    }.toValidatedNel

  private def validateScheduleCron(c: ComponentDescriptor): ValidatedNel[ValidationErrorType, String] = c.specific
    .hcursor.downField(Constants.SCHEDULE_CRON_FIELD).as[String].leftMap { error =>
      logger.error("Error in validateScheduleCron", error)
      InvalidScheduleCron(Constants.SCHEDULE_CRON_FIELD, error)
    }.toValidatedNel

  private def sourceFileExists(mwaaFields: MwaaFields): ValidatedNel[ValidationErrorType, MwaaFields] = s3Gateway
    .objectExists(mwaaFields.bucketName, s"${mwaaFields.sourcePath}${mwaaFields.prefix}${mwaaFields.dagName}")
    .leftMap { error =>
      logger.error(s"Error in sourceFileExists ${error.show}")
      ErrorSourceFile(
        mwaaFields.bucketName,
        s"${mwaaFields.sourcePath}${mwaaFields.prefix}${mwaaFields.dagName}",
        error
      )
    }.toValidatedNel.andThen(exists =>
      Validated.condNel(
        exists,
        mwaaFields,
        MissingSourceFile(mwaaFields.bucketName, s"${mwaaFields.sourcePath}${mwaaFields.prefix}${mwaaFields.dagName}")
      )
    )

  override def validate(descriptor: String): ValidatedNel[ValidationErrorType, MwaaFields] =
    ProvisioningRequestDescriptor(descriptor).leftMap(errors => InvalidDescriptor(errors)).toValidatedNel
      .andThen(provisioningRequestDescriptor =>
        validateComponent(provisioningRequestDescriptor).andThen { c =>
          (
            validateDagName(c),
            validateDestinationPath(c),
            validateSourcePath(c),
            validateBucketName(c),
            validateComponentId(c),
            validateScheduleCron(c)
          ).mapN((dagName, destinationPath, sourcePath, bucketName, prefix, _) =>
            MwaaFields(dagName, c, destinationPath, sourcePath, bucketName, prefix)
          ).andThen(mwaaFields => sourceFileExists(mwaaFields))
        }
      )

}
