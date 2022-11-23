package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3Gateway
import it.agilelab.datamesh.mwaaspecificprovisioner.common.Constants
import it.agilelab.datamesh.mwaaspecificprovisioner.model.{ComponentDescriptor, ProvisioningRequestDescriptor}

class MwaaManager(s3Client: S3Gateway) extends LazyLogging {

  def executeProvision(descriptor: ProvisioningRequestDescriptor): Either[Product, Unit] = {
    logger.info("Starting executing executeProvision method")
    for {
      dagName         <- getDagName(descriptor)
      component       <- getComponent(descriptor)
      destinationPath <- getDestinationPath(descriptor)
      sourcePath      <- getSourcePath(descriptor)
      bucketName      <- getBucketName(descriptor)
      urnArray = component.id.split(":")
      prefix   = s"${urnArray(3)}.${urnArray(4)}.${urnArray(5)}."
      _ <- s3Client
        .copyObject(bucketName, s"$destinationPath$prefix$dagName", bucketName, s"$sourcePath$prefix$dagName")
    } yield ()
  }

  def executeUnprovision(descriptor: ProvisioningRequestDescriptor): Either[Product, Unit] = {
    logger.info("Starting executing executeUnprovision method")
    for {
      dagName         <- getDagName(descriptor)
      bucketName      <- getBucketName(descriptor)
      destinationPath <- getDestinationPath(descriptor)
      component       <- getComponent(descriptor)
      urnArray = component.id.split(":")
      prefix   = s"${urnArray(3)}.${urnArray(4)}.${urnArray(5)}."
      _ <- s3Client.deleteObject(bucketName, s"$destinationPath$prefix$dagName")
    } yield ()
  }

  def getDagName(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getDagName method")
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetDagName(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.DAG_NAME_FIELD).as[String]
        .leftMap(error => GetDagName(descriptor, error.getMessage))
    } yield dagName
  }

  def getBucketName(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getBucketName method")
    for {
      component    <- getComponent(descriptor)
      sourceBucket <- component.specific.hcursor.downField(Constants.BUCKET_NAME_FIELD).as[String]
        .leftMap(error => GetBucketNameError(descriptor, error.getMessage))
    } yield sourceBucket
  }

  def getDestinationPath(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getDestinationPath method")
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetDestinationPathError(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.DESTINATION_DAG_PATH_FIELD).as[String]
        .leftMap(error => GetDestinationPathError(descriptor, error.getMessage))
    } yield dagName
  }

  def getSourcePath(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getSourcePath method")
    for {
      component    <- getComponent(descriptor)
      sourceBucket <- component.specific.hcursor.downField(Constants.SOURCE_DAG_PATH_FIELD).as[String]
        .leftMap(error => GetSourcePathError(descriptor, error.getMessage))
    } yield sourceBucket
  }

  private def getComponent(
      descriptor: ProvisioningRequestDescriptor
  ): Either[MwaaManagerError with Product, ComponentDescriptor] = descriptor.getComponentToProvision
    .toRight(GetComponentError(descriptor, "Unable to find the component to provision"))
}
