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
      componentName     <- getComponentName(descriptor)
      dagName           <- getDagName(descriptor)
      sourceBucket      <- getSourceBucket(descriptor)
      destinationBucket <- getDestinationBucket(descriptor)
      _                 <- s3Client.copyObject(destinationBucket, s"$componentName-$dagName", sourceBucket, dagName)
    } yield ()
  }

  def executeUnprovision(descriptor: ProvisioningRequestDescriptor): Either[Product, Unit] = {
    logger.info("Starting executing executeUnprovision method")
    for {
      componentName     <- getComponentName(descriptor)
      dagName           <- getDagName(descriptor)
      destinationBucket <- getDestinationBucket(descriptor)
      _                 <- s3Client.deleteObject(destinationBucket, s"$componentName-$dagName")
    } yield ()
  }

  def getComponentName(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getComponentName method")
    for {
      component       <- getComponent(descriptor)
      destinationPath <- component.getName.leftMap(error => GetComponentNameError(descriptor, error))
    } yield destinationPath
  }

  def getDestinationBucket(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getDestinationBucket method")
    for {
      component       <- getComponent(descriptor)
      destinationPath <- component.specific.hcursor.downField("destinationBucket").as[String]
        .leftMap(error => GetDestinationBucketError(descriptor, error.getMessage))
    } yield destinationPath
  }

  def getSourceBucket(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getSourceBucket method")
    for {
      component    <- getComponent(descriptor)
      sourceBucket <- component.specific.hcursor.downField("sourceBucket").as[String]
        .leftMap(error => GetSourceBucketError(descriptor, error.getMessage))
    } yield sourceBucket
  }

  def getDagName(descriptor: ProvisioningRequestDescriptor): Either[GetDagNameError, String] = {
    logger.info("Starting executing getDagName method")
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetDagNameError(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.DAG_NAME_FIELD).as[String]
        .leftMap(error => GetDagNameError(descriptor, error.getMessage))
    } yield dagName
  }

  private def getComponent(
      descriptor: ProvisioningRequestDescriptor
  ): Either[MwaaManagerError with Product, ComponentDescriptor] = descriptor.getComponentToProvision
    .toRight(GetComponentError(descriptor, "Unable to find the component to provision"))
}
