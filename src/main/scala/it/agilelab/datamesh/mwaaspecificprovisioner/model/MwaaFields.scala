package it.agilelab.datamesh.mwaaspecificprovisioner.model

case class MwaaFields(
    dagName: String,
    component: ComponentDescriptor,
    destinationPath: String,
    sourcePath: String,
    bucketName: String,
    prefix: String
)
