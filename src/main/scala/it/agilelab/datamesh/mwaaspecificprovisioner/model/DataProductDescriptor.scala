package it.agilelab.datamesh.mwaaspecificprovisioner.model

import io.circe.{HCursor, Json}
import io.circe.yaml.parser
import it.agilelab.datamesh.mwaaspecificprovisioner.common.Constants

final case class DataProductDescriptor(
    id: String,
    environment: String,
    header: Json,
    components: List[ComponentDescriptor]
) extends YamlPrinter {

  override def toString: String = s"${printAsYaml(toJson)}"

  def toJson: Json = Json.obj((Constants.DATA_PRODUCT_FIELD, header))

  def getName: Either[String, String] = header.hcursor.downField(Constants.NAME_FIELD).as[String].left
    .map(_ => s"cannot parse Data Product name for ${header.spaces2}")

}

object DataProductDescriptor {

  private def getId(header: Json): Either[String, String] = header.hcursor.downField(Constants.ID_FIELD).as[String].left
    .map(_ => s"cannot parse Data Product id for ${header.spaces2}")

  private def getEnvironment(header: Json): Either[String, String] = header.hcursor
    .downField(Constants.ENVIRONMENT_FIELD).as[String].left
    .map(_ => s"cannot parse Data Product environment for ${header.spaces2}")

  private def getDpHeaderDescriptor(hcursor: HCursor): Either[String, Json] =
    hcursor.downField(Constants.DATA_PRODUCT_FIELD).focus match {
      case Some(x) => Right(x)
      case None    => Left(s"cannot parse Data Product header for ${hcursor.value.spaces2}")
    }

  private def getComponentsDescriptor(environment: String, header: Json): Either[String, List[ComponentDescriptor]] = {
    val componentsHCursor = header.hcursor.downField(Constants.COMPONENTS_FIELD)
    componentsHCursor.values.map(_.toList)
  } match {
    case None           => Left(s"cannot parse Data Product components for ${header.spaces2}")
    case Some(jsonList) =>
      val result = jsonList.map(c => ComponentDescriptor(environment, c))
      result.collectFirst { case Left(error) => error }.toLeft(result.collect { case Right(r) => r })
  }

  def apply(yaml: String): Either[String, DataProductDescriptor] = parser.parse(yaml) match {
    case Left(err)   => Left(err.getMessage())
    case Right(json) =>
      val hcursor                                        = json.hcursor
      val maybeDp: Either[String, DataProductDescriptor] = for {
        header      <- getDpHeaderDescriptor(hcursor.root)
        id          <- getId(header)
        environment <- getEnvironment(header)
        components  <- getComponentsDescriptor(environment, header)
      } yield DataProductDescriptor(id, environment, header, components)

      maybeDp match {
        case Left(errorMsg) => Left(errorMsg)
        case Right(dp)      => Right(dp)
      }
  }
}
