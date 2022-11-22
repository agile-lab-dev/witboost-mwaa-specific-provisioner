package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import it.agilelab.datamesh.mwaaspecificprovisioner.model.ProvisioningRequestDescriptor

sealed trait MwaaManagerError {
  def errorMessage: String
}

final case class SendRequestError(dagName: String, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"SendRequestError($dagName, $error)"
}

final case class GetDagName(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetDagName($descriptor, $error)"
}

final case class GetBucketNameError(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetBucketNameError($descriptor, $error)"
}

final case class GetDestinationPathError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"GetDestinationPathError($descriptor, $error)"
}

final case class GetSourcePathError(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetSourcePathError($descriptor, $error)"
}

final case class GetComponentError(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetComponentError($descriptor, $error)"
}

final case class GetComponentNameError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"GetComponentNameError($descriptor, $error)"
}

final case class ExecuteProvisionError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"ExecuteProvisionError($descriptor, $error)"
}
