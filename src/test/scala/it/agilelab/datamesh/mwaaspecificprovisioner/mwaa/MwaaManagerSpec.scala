package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import cats.data.Validated
import cats.data.Validated.Valid
import io.circe.Json
import it.agilelab.datamesh.mwaaspecificprovisioner.error.InvalidDagName
import it.agilelab.datamesh.mwaaspecificprovisioner.model.{ComponentDescriptor, MwaaFields}
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3Gateway
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3GatewayError.S3GatewayInitError
import it.agilelab.datamesh.mwaaspecificprovisioner.validation.Validator
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MwaaManagerSpec extends AnyFlatSpec with MockFactory with Matchers {

  private val s3Client  = mock[S3Gateway]
  private val validator = mock[Validator]

  "Validation" should "succeed" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _           = (validator.validate _).expects(*)
      .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))
    val _           = (s3Client.copyObject _).expects(*, *, *, *).never()
    val _           = (s3Client.deleteObject _).expects(*, *).never()

    val res = mwaaManager.executeValidation("valid")

    res.isValid should be(true)
  }

  "Provision" should "succeed" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _           = (validator.validate _).expects(*)
      .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))
    val _           = (s3Client.copyObject _).expects(*, *, *, *).returns(Right(()))

    val res = mwaaManager.executeProvision("valid")

    res.isValid should be(true)
  }

  it should "fail if validation fails" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _ = (validator.validate _).expects(*).returns(Validated.invalidNel(InvalidDagName("", new Throwable(""))))
    val _ = (s3Client.copyObject _).expects(*, *, *, *).never()

    val res = mwaaManager.executeProvision("invalid")

    res.isValid should be(false)
  }

  it should "fail if copy fails" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _           = (validator.validate _).expects(*)
      .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))
    val _           = (s3Client.copyObject _).expects(*, *, *, *).returns(Left(S3GatewayInitError(new Throwable(""))))

    val res = mwaaManager.executeProvision("valid")

    res.isValid should be(false)
  }

  "Unprovision" should "succeed" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _           = (validator.validate _).expects(*)
      .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))
    val _           = (s3Client.deleteObject _).expects(*, *).returns(Right(()))

    val res = mwaaManager.executeUnprovision("valid")

    res.isValid should be(true)
  }

  it should "fail if validation fails" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _ = (validator.validate _).expects(*).returns(Validated.invalidNel(InvalidDagName("", new Throwable(""))))
    val _ = (s3Client.deleteObject _).expects(*, *).never()

    val res = mwaaManager.executeUnprovision("invalid")

    res.isValid should be(false)
  }

  it should "fail if delete fails" in {
    val mwaaManager = new MwaaManager(s3Client, validator)
    val _           = (validator.validate _).expects(*)
      .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))
    val _           = (s3Client.deleteObject _).expects(*, *).returns(Left(S3GatewayInitError(new Throwable(""))))

    val res = mwaaManager.executeUnprovision("valid")

    res.isValid should be(false)
  }

}
