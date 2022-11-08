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
      destinationDag    <- getDestinationDag(descriptor)
      sourceDag         <- getSourceDag(descriptor)
      sourceBucket      <- getSourceBucket(descriptor)
      destinationBucket <- getDestinationBucket(descriptor)
      _                 <- s3Client.copyObject(destinationBucket, destinationDag, sourceBucket, sourceDag)
    } yield ()
  }

  def executeUnprovision(descriptor: ProvisioningRequestDescriptor): Either[Product, Unit] = {
    logger.info("Starting executing executeUnprovision method")
    for {
      dagName           <- getDestinationDag(descriptor)
      destinationBucket <- getDestinationBucket(descriptor)
      _                 <- s3Client.deleteObject(destinationBucket, dagName)
    } yield ()
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

  def getDestinationDag(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getDestinationDag method")
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetDestinationDagError(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.DESTINATION_DAG_FIELD).as[String]
        .leftMap(error => GetDestinationDagError(descriptor, error.getMessage))
    } yield dagName
  }

  def getSourceDag(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing getSourceDag method")
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetSourceDagError(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.SOURCE_DAG_FIELD).as[String]
        .leftMap(error => GetSourceDagError(descriptor, error.getMessage))
    } yield dagName
  }

  private def getComponent(
      descriptor: ProvisioningRequestDescriptor
  ): Either[MwaaManagerError with Product, ComponentDescriptor] = descriptor.getComponentToProvision
    .toRight(GetComponentError(descriptor, "Unable to find the component to provision"))
}
