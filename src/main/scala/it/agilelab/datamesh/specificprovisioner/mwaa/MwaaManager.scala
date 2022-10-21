package it.agilelab.datamesh.specificprovisioner.mwaa

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{headers, ContentTypes, HttpEntity, HttpMethods, HttpRequest, ResponseEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import cats.implicits.toBifunctorOps
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax.EncoderOps
import it.agilelab.datamesh.specificprovisioner.common.Constants
import it.agilelab.datamesh.specificprovisioner.system.ApplicationConfiguration.{
  airflowBaseUrl,
  functionsInvocationTimeout,
  password,
  user
}
import it.agilelab.datamesh.specificprovisioner.model.ProvisioningRequestDescriptor

import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class MwaaManager extends LazyLogging {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  def executeProvision(descriptor: ProvisioningRequestDescriptor): Either[MwaaManagerError with Product, String] = {
    logger.info("Starting executing executeProvision method with descriptor: {}", descriptor)
    for {
      dagName  <- getDagName(descriptor)
      response <- sendRequest(dagName)
    } yield response
  }

  def getDagName(descriptor: ProvisioningRequestDescriptor): Either[GetDagNameError, String] = {
    logger.info("Starting executing getDagName method with descriptor: {}", descriptor)
    for {
      component <- descriptor.getComponentToProvision
        .toRight(GetDagNameError(descriptor, "Unable to find the component to provision"))
      dagName   <- component.specific.hcursor.downField(Constants.DAG_NAME_FIELD).as[String]
        .leftMap(error => GetDagNameError(descriptor, error.getMessage))
    } yield dagName
  }

  def sendRequest(dagName: String): Either[SendRequestError, String] = {
    logger.info("Starting unpausing dag: {}", dagName)
    val response = for {
      response       <- Http().singleRequest(buildRequest(dagName))
      parsedResponse <- parseResponse(response.entity)
    } yield parsedResponse

    Try(Await.result(response, functionsInvocationTimeout)) match {
      case Failure(exception)      => Left(SendRequestError(dagName, s"Error: $exception"))
      case Success(parsedResponse) => Right(parsedResponse)
    }
  }

  private def parseResponse(responseEntity: ResponseEntity): Future[String] = Unmarshal(responseEntity).to[String]

  private def buildRequest(dagName: String): HttpRequest = HttpRequest(
    method = HttpMethods.PATCH,
    uri = s"$airflowBaseUrl/dags/$dagName",
    entity = HttpEntity(ContentTypes.`application/json`, DagModel(false).asJson.toString)
  ).withHeaders(headers.RawHeader(
    "Authorization",
    s"Basic ${Base64.getEncoder.encodeToString(s"$user:$password".getBytes(StandardCharsets.UTF_8))}"
  ))
}
