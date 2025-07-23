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

  def sourceObjectLocation = s"${sourcePath.ensureTrailingSlash}$prefix.$dagName"

  def destinationObjectLocation: String =
    s"${destinationPath.ensureTrailingSlash}${prefix.replaceAll("[^a-zA-Z0-9_]", "_")}_" + {
      if (dagName.endsWith(".py")) dagName.stripSuffix(".py").replaceAll("[^a-zA-Z0-9_]", "_") + ".py"
      else dagName.replaceAll("[^a-zA-Z0-9_]", "_")
    }

}
