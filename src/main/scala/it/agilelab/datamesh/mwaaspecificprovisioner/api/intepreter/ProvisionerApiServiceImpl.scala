package it.agilelab.datamesh.mwaaspecificprovisioner.api.intepreter

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import cats.data.NonEmptyList
import cats.implicits.toShow
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport.{marshaller, unmarshaller}
import it.agilelab.datamesh.mwaaspecificprovisioner.api.SpecificProvisionerApiService
import it.agilelab.datamesh.mwaaspecificprovisioner.model._
import it.agilelab.datamesh.mwaaspecificprovisioner.mwaa.{MwaaManager, MwaaManagerError}
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.{S3Gateway, S3GatewayError}

class ProvisionerApiServiceImpl(s3Client: S3Gateway) extends SpecificProvisionerApiService with LazyLogging {

  // Json String
  implicit val toEntityMarshallerJsonString: ToEntityMarshaller[String]       = marshaller[String]
  implicit val toEntityUnmarshallerJsonString: FromEntityUnmarshaller[String] = unmarshaller[String]

  implicit val toEntityMarshallerJsonBoolean: ToEntityMarshaller[Boolean]       = marshaller[Boolean]
  implicit val toEntityUnmarshallerJsonBoolean: FromEntityUnmarshaller[Boolean] = unmarshaller[Boolean]

  private val mwaaManager = new MwaaManager(s3Client)

  /** Code: 200, Message: The request status, DataType: Status
   *  Code: 400, Message: Invalid input, DataType: ValidationError
   *  Code: 500, Message: System problem, DataType: SystemError
   */
  override def getStatus(token: String)(implicit
      contexts: Seq[(String, String)],
      toEntityMarshallerSystemError: ToEntityMarshaller[SystemError],
      toEntityMarshallerProvisioningStatus: ToEntityMarshaller[ProvisioningStatus],
      toEntityMarshallerValidationError: ToEntityMarshaller[ValidationError]
  ): Route = getStatus200(ProvisioningStatus(ProvisioningStatusEnums.StatusEnum.COMPLETED, Some("Ok")))

  /** Code: 200, Message: It synchronously returns the request result, DataType: ProvisioningStatus
   *  Code: 202, Message: If successful returns a provisioning deployment task token that can be used for polling the request status, DataType: String
   *  Code: 400, Message: Invalid input, DataType: ValidationError
   *  Code: 500, Message: System problem, DataType: SystemError
   */
  override def provision(provisioningRequest: ProvisioningRequest)(implicit
      contexts: Seq[(String, String)],
      toEntityMarshallerSystemError: ToEntityMarshaller[SystemError],
      toEntityMarshallerProvisioningStatus: ToEntityMarshaller[ProvisioningStatus],
      toEntityMarshallerValidationError: ToEntityMarshaller[ValidationError]
  ): Route = ProvisioningRequestDescriptor(provisioningRequest.descriptor).flatMap(mwaaManager.executeProvision) match {
    case Left(e: S3GatewayError)   =>
      logger.error(e.show)
      provision500(SystemError(e.show))
    case Left(e: MwaaManagerError) =>
      logger.error(e.errorMessage)
      provision500(SystemError(e.errorMessage))
    case Left(e: NonEmptyList[_])  =>
      logger.error(e.head.toString)
      provision400(ValidationError(e.toList.map(_.toString)))
    case Right(_)                  =>
      logger.info("OK")
      provision202("OK")
    case _                         =>
      logger.error("Generic Error")
      provision500(SystemError("Generic Error"))
  }

  /** Code: 200, Message: It synchronously returns the request result, DataType: String
   *  Code: 400, Message: Invalid input, DataType: ValidationError
   *  Code: 500, Message: System problem, DataType: SystemError
   */
  override def validate(provisioningRequest: ProvisioningRequest)(implicit
      contexts: Seq[(String, String)],
      toEntityMarshallerSystemError: ToEntityMarshaller[SystemError],
      toEntityMarshallerValidationResult: ToEntityMarshaller[ValidationResult]
  ): Route = validate200(ValidationResult(valid = true))

  /** Code: 200, Message: It synchronously returns the request result, DataType: ProvisioningStatus
   *  Code: 202, Message: If successful returns a provisioning deployment task token that can be used for polling the request status, DataType: String
   *  Code: 400, Message: Invalid input, DataType: ValidationError
   *  Code: 500, Message: System problem, DataType: SystemError
   */
  override def unprovision(provisioningRequest: ProvisioningRequest)(implicit
      contexts: Seq[(String, String)],
      toEntityMarshallerSystemError: ToEntityMarshaller[SystemError],
      toEntityMarshallerProvisioningStatus: ToEntityMarshaller[ProvisioningStatus],
      toEntityMarshallerValidationError: ToEntityMarshaller[ValidationError]
  ): Route =
    ProvisioningRequestDescriptor(provisioningRequest.descriptor).flatMap(mwaaManager.executeUnprovision) match {
      case Left(e: S3GatewayError)   =>
        logger.error(e.show)
        provision500(SystemError(e.show))
      case Left(e: MwaaManagerError) =>
        logger.error(e.errorMessage)
        provision500(SystemError(e.errorMessage))
      case Left(e: NonEmptyList[_])  =>
        logger.error(e.head.toString)
        provision400(ValidationError(e.toList.map(_.toString)))
      case Right(_)                  =>
        logger.info("OK")
        provision202("OK")
      case _                         =>
        logger.error("Generic Error")
        provision500(SystemError("Generic Error"))
    }

  /** Code: 200, Message: It synchronously returns the access request response, DataType: ProvisioningStatus
   *  Code: 202, Message: It synchronously returns the access request response, DataType: String
   *  Code: 400, Message: Invalid input, DataType: ValidationError
   *  Code: 500, Message: System problem, DataType: SystemError
   */
  override def updateacl(updateAclRequest: UpdateAclRequest)(implicit
      contexts: Seq[(String, String)],
      toEntityMarshallerSystemError: ToEntityMarshaller[SystemError],
      toEntityMarshallerProvisioningStatus: ToEntityMarshaller[ProvisioningStatus],
      toEntityMarshallerValidationError: ToEntityMarshaller[ValidationError]
  ): Route = updateacl202("OK")
}
