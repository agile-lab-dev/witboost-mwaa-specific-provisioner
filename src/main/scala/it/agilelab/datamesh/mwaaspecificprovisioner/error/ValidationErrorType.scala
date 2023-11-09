package it.agilelab.datamesh.mwaaspecificprovisioner.error

import cats.data.NonEmptyList

trait ValidationErrorType extends ErrorType

case class InvalidDescriptor(errors: NonEmptyList[String]) extends ValidationErrorType {
  override def errorMessage: String = s"Descriptor is not valid. Details: ${errors.toList.mkString(",")}"
}

case class InvalidComponent(componentId: String) extends ValidationErrorType {
  override def errorMessage: String = s"The component '$componentId' to provision is not present"
}

case class InvalidComponentId(componentId: String) extends ValidationErrorType {
  override def errorMessage: String = s"The componentId '$componentId' is not valid"
}

case class InvalidDagName(fieldName: String, error: Throwable) extends ValidationErrorType {
  override def errorMessage: String = s"The $fieldName field is not present or is invalid. Details: ${error.getMessage}"
}

case class InvalidDestinationPath(fieldName: String, error: Throwable) extends ValidationErrorType {
  override def errorMessage: String = s"The $fieldName field is not present or is invalid. Details: ${error.getMessage}"
}

case class InvalidSourcePath(fieldName: String, error: Throwable) extends ValidationErrorType {
  override def errorMessage: String = s"The $fieldName field is not present or is invalid. Details: ${error.getMessage}"
}

case class InvalidBucketName(fieldName: String, error: Throwable) extends ValidationErrorType {
  override def errorMessage: String = s"The $fieldName field is not present or is invalid. Details: ${error.getMessage}"
}

case class InvalidScheduleCron(fieldName: String, error: Throwable) extends ValidationErrorType {
  override def errorMessage: String = s"The $fieldName field is not present or is invalid. Details: ${error.getMessage}"
}

case class ErrorSourceFile(bucket: String, key: String, error: Throwable) extends ValidationErrorType {

  override def errorMessage: String =
    s"An error occurred while verifying existence of the file $key in bucket $bucket: ${error.getMessage}"
}

case class MissingSourceFile(bucket: String, key: String) extends ValidationErrorType {
  override def errorMessage: String = s"File $key not found in bucket $bucket"
}
