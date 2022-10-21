package it.agilelab.datamesh.specificprovisioner.mwaa

import it.agilelab.datamesh.specificprovisioner.model.ProvisioningRequestDescriptor

sealed trait MwaaManagerError {
  def errorMessage: String
}

final case class SendRequestError(dagName: String, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"SendRequestError($dagName, $error)"
}

final case class GetDagNameError(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetDagNameError($descriptor, $error)"
}
