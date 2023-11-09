package it.agilelab.datamesh.mwaaspecificprovisioner.server.impl

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.directives.{Credentials, SecurityDirectives}
import buildinfo.BuildInfo
import com.typesafe.scalalogging.LazyLogging
import it.agilelab.datamesh.mwaaspecificprovisioner.api.SpecificProvisionerApi
import it.agilelab.datamesh.mwaaspecificprovisioner.api.intepreter.{
  ProvisionerApiMarshallerImpl,
  ProvisionerApiServiceImpl
}
import it.agilelab.datamesh.mwaaspecificprovisioner.mwaa.MwaaManager
import it.agilelab.datamesh.mwaaspecificprovisioner.s3.gateway.{S3Gateway, S3GatewayMock}
import it.agilelab.datamesh.mwaaspecificprovisioner.server.Controller
import it.agilelab.datamesh.mwaaspecificprovisioner.system.ApplicationConfiguration.{httpPort, isMock}
import it.agilelab.datamesh.mwaaspecificprovisioner.validation.MwaaValidator

import scala.jdk.CollectionConverters._

object Main extends LazyLogging {

  def run(port: Int): ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      import akka.actor.typed.scaladsl.adapter._
      implicit val classicSystem: actor.ActorSystem = context.system.toClassic

      val validator = new MwaaValidator(clientAws)
      val manager   = new MwaaManager(clientAws, validator)
      val impl      = new ProvisionerApiServiceImpl(manager)

      val api = new SpecificProvisionerApi(
        impl,
        new ProvisionerApiMarshallerImpl(),
        SecurityDirectives.authenticateBasic("SecurityRealm", (_: Credentials) => Some(Seq.empty[(String, String)]))
      )

      val controller = new Controller(
        api,
        validationExceptionToRoute = Some { e =>
          logger.error(s"Error: ", e)
          val results = e.results()
          if (Option(results).isDefined) {
            results.crumbs().asScala.foreach(crumb => logger.info(crumb.crumb()))
            results.items().asScala.foreach { item =>
              logger.info(item.dataCrumbs())
              logger.info(item.dataJsonPointer())
              logger.info(item.schemaCrumbs())
              logger.info(item.message())
              logger.info("Severity: ", item.severity())
            }
            val message = e.results().items().asScala.map(_.message()).mkString("\n")
            complete((400, message))
          } else complete((400, e.getMessage))
        }
      )

      val _ = Http().newServerAt("0.0.0.0", port).bind(controller.routes)
      Behaviors.empty
    },
    BuildInfo.name.replaceAll("""\.""", "-")
  )

  @SuppressWarnings(Array("scalafix:DisableSyntax.throw"))
  def clientAws: S3Gateway =
    if (isMock) { new S3GatewayMock }
    else {
      S3Gateway.apply match {
        case Left(exception) =>
          logger.error("Error: ", exception)
          throw exception
        case Right(value)    => value
      }
    }

  def main(args: Array[String]): Unit = { val _ = run(httpPort) }
}
