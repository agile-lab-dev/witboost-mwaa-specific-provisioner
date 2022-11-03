package it.agilelab.datamesh.mwaaspecificprovisioner.model

sealed trait ProvisioningRequestDescriptorError {
  def errorMessage: String
}

final case class ApplyProvisioningRequestDescriptorError(yaml: String, error: String)
    extends ProvisioningRequestDescriptorError {
  override def errorMessage: String = s"ApplyProvisioningRequestDescriptorError($yaml, $error)"
}
