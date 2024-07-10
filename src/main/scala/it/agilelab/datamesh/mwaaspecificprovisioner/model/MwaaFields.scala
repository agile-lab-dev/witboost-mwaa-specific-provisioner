package it.agilelab.datamesh.mwaaspecificprovisioner.model

import it.agilelab.datamesh.mwaaspecificprovisioner.common.StringUtils.StringImplicits

case class MwaaFields(
    dagName: String,
    component: ComponentDescriptor,
    destinationPath: String,
    sourcePath: String,
    bucketName: String,
    prefix: String
) {

  def sourceObjectLocation      = s"${sourcePath.ensureTrailingSlash}$prefix.$dagName"
  def destinationObjectLocation = s"${destinationPath.ensureTrailingSlash}$prefix.$dagName"

}
