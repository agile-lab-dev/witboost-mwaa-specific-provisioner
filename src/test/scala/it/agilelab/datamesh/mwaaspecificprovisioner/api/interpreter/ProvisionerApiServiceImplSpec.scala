package it.agilelab.datamesh.mwaaspecificprovisioner.api.interpreter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive1, RequestContext, Route}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestDuration
import cats.data.{NonEmptyList, Validated}
import cats.data.Validated.Valid
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import it.agilelab.datamesh.mwaaspecificprovisioner.api.SpecificProvisionerApi
import it.agilelab.datamesh.mwaaspecificprovisioner.api.intepreter.{
  ProvisionerApiMarshallerImpl,
  ProvisionerApiServiceImpl
}
import it.agilelab.datamesh.mwaaspecificprovisioner.common.test.getTestResourceAsString
import it.agilelab.datamesh.mwaaspecificprovisioner.error.{InvalidDagName, InvalidDescriptor, ProvisionErrorType}
import it.agilelab.datamesh.mwaaspecificprovisioner.model._
import it.agilelab.datamesh.mwaaspecificprovisioner.mwaa.MwaaManager
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.S3GatewayError.S3GatewayInitError
import it.agilelab.datamesh.mwaaspecificprovisioner.server.Controller
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest, OptionValues}

import scala.concurrent.duration.DurationInt

class ProvisionerApiServiceImplSpec
    extends AnyFlatSpec
    with ScalatestRouteTest
    with Matchers
    with BeforeAndAfterAll
    with StrictLogging
    with FailFastCirceSupport
    with OptionValues
    with MockFactory
    with OneInstancePerTest {

  implicit val actorSystem: ActorSystem[_] = ActorSystem[Nothing](Behaviors.empty, "ProvisionerApiServiceImplSpec")

  implicit def default(implicit system: ActorSystem[Nothing]): RouteTestTimeout =
    RouteTestTimeout(new DurationInt(5).second.dilated(system.classicSystem))

  val marshaller = new ProvisionerApiMarshallerImpl

  val mwaaManager = mock[MwaaManager]

  val api = new SpecificProvisionerApi(
    new ProvisionerApiServiceImpl(mwaaManager),
    new ProvisionerApiMarshallerImpl,
    new ExtractContexts {

      override def tapply(f: (Tuple1[Seq[(String, String)]]) => Route): Route = { (rc: RequestContext) =>
        f(Tuple1(Seq.empty))(rc)
      }
    }
  )

  new Controller(api, validationExceptionToRoute = None)(system)

  import marshaller._

  implicit val contexts: Seq[(String, String)] = Seq.empty

  "ProvisionerApiServiceImpl" should
    "synchronously validate with no errors when a valid descriptor is passed as input" in {
      val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = "valid", removeData = false)

      val _ = (mwaaManager.executeValidation _).expects(*)
        .returns(Valid(MwaaFields("", ComponentDescriptor("", "", Json.Null, Json.Null), "", "", "", "")))

      Post("/v1/validate", request) ~> api.route ~> check {
        val response = responseAs[ValidationResult]
        response.valid shouldEqual true
        response.error shouldBe None
      }
    }

  it should "return a validation error if validation fails" in {
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = "invalid", removeData = false)

    val _ = (mwaaManager.executeValidation _).expects(*)
      .returns(Validated.invalidNel(InvalidDagName("", new Throwable(""))))

    Post("/v1/validate", request) ~> api.route ~> check {
      val response = responseAs[ValidationResult]
      response.valid shouldEqual false
      response.error.isDefined shouldBe true
    }
  }

  it should "raise an error if there's an uncaught exception while validating" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeValidation _).expects(*).throws(new Throwable(""))

    Post("/v1/validate", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.InternalServerError)
  }

  it should "synchronously provision when a valid descriptor is passed as input" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeProvision _).expects(*).returns(Valid(()))

    Post("/v1/provision", request) ~> api.route ~> check {
      val response = responseAs[ProvisioningStatus]
      response.status shouldEqual ProvisioningStatusEnums.StatusEnum.COMPLETED
    }
  }

  it should "raise an error if provision received descriptor is not valid" in {
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = "invalid", removeData = false)

    val _ = (mwaaManager.executeProvision _).expects(*)
      .returns(Validated.invalidNel(InvalidDescriptor(NonEmptyList.one(""))))

    Post("/v1/provision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.BadRequest)
  }

  it should "raise an error if underlying provisioning fails" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeProvision _).expects(*)
      .returns(Validated.invalidNel(ProvisionErrorType(S3GatewayInitError(new Throwable("")))))

    Post("/v1/provision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.BadRequest)
  }

  it should "raise an error if there's an uncaught exception while provisioning" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeProvision _).expects(*).throws(new Throwable(""))

    Post("/v1/provision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.InternalServerError)
  }

  it should "synchronously unprovision when a valid descriptor is passed as input" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeUnprovision _).expects(*).returns(Valid(()))

    Post("/v1/unprovision", request) ~> api.route ~> check {
      val response = responseAs[ProvisioningStatus]
      response.status shouldEqual ProvisioningStatusEnums.StatusEnum.COMPLETED
    }
  }

  it should "raise an error if unprovision received descriptor is not valid" in {
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = "invalid", removeData = false)

    val _ = (mwaaManager.executeUnprovision _).expects(*)
      .returns(Validated.invalidNel(InvalidDescriptor(NonEmptyList.one(""))))

    Post("/v1/unprovision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.BadRequest)
  }

  it should "raise an error if underlying unprovisioning fails" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeUnprovision _).expects(*)
      .returns(Validated.invalidNel(ProvisionErrorType(S3GatewayInitError(new Throwable("")))))

    Post("/v1/unprovision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.BadRequest)
  }

  it should "raise an error if there's an uncaught exception while unprovisioning" in {
    val yaml    = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = yaml, removeData = false)

    val _ = (mwaaManager.executeUnprovision _).expects(*).throws(new Throwable(""))

    Post("/v1/unprovision", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.InternalServerError)
  }

  it should "raise an error for an updateAcl request" in {
    val request = UpdateAclRequest(List("sergio.mejia_agilelab.it"), ProvisionInfo("req", "res"))

    Post("/v1/updateacl", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.InternalServerError)
  }

  it should "raise an error for an async getStatus request" in Get("/v1/provision/token/status") ~> api.route ~> check {
    response.status shouldEqual StatusCodes.BadRequest
  }

  it should "raise an error for an async validate request" in {
    val request = ProvisioningRequest(DATAPRODUCT_DESCRIPTOR, descriptor = "", removeData = false)

    Post("/v2/validate", request) ~> api.route ~> check(response.status shouldEqual StatusCodes.InternalServerError)
  }

  it should "raise an error for a validate status request" in Get("/v2/validate/token/status") ~> api.route ~> check {
    response.status shouldEqual StatusCodes.InternalServerError
  }

  it should "raise an error for a reverse provisioning request" in {
    val request = ReverseProvisioningRequest("", "")

    Post("/v1/reverse-provisioning", request) ~> api.route ~> check {
      response.status shouldEqual StatusCodes.InternalServerError
    }
  }

  it should "raise an error for a reverse provisioning status request" in
    Get("/v1/reverse-provisioning/token/status") ~> api.route ~> check {
      response.status shouldEqual StatusCodes.InternalServerError
    }

}

abstract class ExtractContexts extends Directive1[Seq[(String, String)]]

object ExtractContexts {

  def apply(other: Directive1[Seq[(String, String)]]): ExtractContexts = inner =>
    other.tapply((seq: Tuple1[Seq[(String, String)]]) =>
      (rc: RequestContext) => {
        val headers = rc.request.headers.map(header => (header.name(), header.value()))
        inner(Tuple1(seq._1 ++ headers))(rc)
      }
    )
}
