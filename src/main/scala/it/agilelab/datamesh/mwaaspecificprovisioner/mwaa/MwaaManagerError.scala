package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import it.agilelab.datamesh.mwaaspecificprovisioner.model.ProvisioningRequestDescriptor

sealed trait MwaaManagerError {
  def errorMessage: String
}

final case class SendRequestError(dagName: String, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"SendRequestError($dagName, $error)"
}

final case class GetDestinationDagError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"GetDestinationDagError($descriptor, $error)"
}

final case class GetSourceDagError(descriptor: ProvisioningRequestDescriptor, error: String) extends MwaaManagerError {
  override def errorMessage: String = s"GetSourceDagError($descriptor, $error)"
}

final case class GetSourceBucketError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"GetSourceBucketError($descriptor, $error)"
}

final case class GetDestinationBucketError(descriptor: ProvisioningRequestDescriptor, error: String)
    extends MwaaManagerError {
  override def errorMessage: String = s"GetDestinationBucketError($descriptor, $error)"
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
