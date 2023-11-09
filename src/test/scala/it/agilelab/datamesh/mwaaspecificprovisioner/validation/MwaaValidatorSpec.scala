package it.agilelab.datamesh.mwaaspecificprovisioner.validation

import cats.data.Validated
import it.agilelab.datamesh.mwaaspecificprovisioner.common.test.getTestResourceAsString
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3GatewayError.ObjectExistsErr
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.{S3Gateway, S3GatewayMock}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MwaaValidatorSpec extends AnyFlatSpec with Matchers with MockFactory {

  "Validation" should "succeed" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ok.yml")
    val s3Gateway = new S3GatewayMock()
    val validator = new MwaaValidator(s3Gateway)

    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(mf)  =>
        mf.dagName should be("airbyte_snowflake_dag_custom_wit_567.py")
        mf.component.id should be("urn:dmb:cmp:demographic:dp-to-test-mwaa-fix:0:airflow-workload-test-fix")
        mf.bucketName should be("sandbox-qa-mwaa-eu-west-1-278435202378")
        mf.sourcePath should be("source/")
        mf.destinationPath should be("dags/")
        mf.prefix should be("demographic.dp-to-test-mwaa-fix.0")
      case Validated.Invalid(_) => fail("Expected valid")
    }
  }

  it should "fail if DP is empty" in {
    val yaml      = ""
    val s3Gateway = new S3GatewayMock()
    val validator = new MwaaValidator(s3Gateway)

    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(1)
    }
  }

  it should "fail if specific fields are missing" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ko_missing_specifics.yml")
    val s3Gateway = new S3GatewayMock()
    val validator = new MwaaValidator(s3Gateway)

    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(5)
    }
  }

  it should "fail if the component is missing" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ko_missing_component.yml")
    val s3Gateway = new S3GatewayMock()
    val validator = new MwaaValidator(s3Gateway)

    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(1)
    }
  }

  it should "fail if the componentId hasn't the expected format" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ko_wrong_component_id_format.yml")
    val s3Gateway = new S3GatewayMock()
    val validator = new MwaaValidator(s3Gateway)

    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(1)
    }
  }

  it should "fail if the source file is not existent" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ok.yml")
    val s3Gateway = mock[S3Gateway]
    val validator = new MwaaValidator(s3Gateway)

    val _   = (s3Gateway.objectExists _).expects(*, *).returns(Right(false))
    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(1)
    }
  }

  it should "fail if s3Gateway.objectExists returns a Left" in {
    val yaml      = getTestResourceAsString("validation/descriptor_ok.yml")
    val s3Gateway = mock[S3Gateway]
    val validator = new MwaaValidator(s3Gateway)

    val _   = (s3Gateway.objectExists _).expects(*, *).returns(Left(ObjectExistsErr("", "", new Throwable(""))))
    val res = validator.validate(yaml)

    res match {
      case Validated.Valid(_)   => fail("Expected invalid")
      case Validated.Invalid(e) => e.length should be(1)
    }
  }

}
