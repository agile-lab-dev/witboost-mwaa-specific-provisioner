package it.agilelab.datamesh.mwaaspecificprovisioner.error

import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3GatewayError

case class ProvisionErrorType(error: S3GatewayError) extends ErrorType {

  override def errorMessage: String =
    s"An error occurred while provisioning/unprovisioning the component. Details: ${error.getMessage}"
}
