package it.agilelab.datamesh.mwaaspecificprovisioner.error

case class GenericErrorType(message: String) extends ErrorType {

  override def errorMessage: String =
    s"An error occurred while provisioning/unprovisioning the component. Details: $message"
}
