package it.agilelab.datamesh.mwaaspecificprovisioner.model

import cats.data.{EitherNel, NonEmptyList}
import it.agilelab.datamesh.mwaaspecificprovisioner.common.test.getTestResourceAsString
import org.scalatest.EitherValues._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class DescriptorParserSpec extends AnyFlatSpec {

  "Parsing a well formed descriptor" should "return a correct DataProductDescriptor" in {
    val yaml = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")

    val prDescr = ProvisioningRequestDescriptor(yaml)

    val dpDescr = prDescr.toOption.get.dataProduct

    val backYaml = dpDescr.toString

    val backDpDescr = DataProductDescriptor(backYaml).toOption.get

    dpDescr should be(backDpDescr)
  }

  "Parsing a well formed descriptor" should "return a correct ComponentToProvision" in {
    val yaml = getTestResourceAsString("pr_descriptors/pr_descriptor_1.yml")

    val prDescr = ProvisioningRequestDescriptor(yaml)

    val component = prDescr.toOption.get.getComponentToProvision

    component should be(Symbol("defined"))
  }

  "Parsing a wrongly formed descriptor with missing component id field" should "return a Left with a Exception" in {

    val yaml = getTestResourceAsString("pr_descriptors/pr_descriptor_1_missing_component_id.yml")

    val dp: EitherNel[String, ProvisioningRequestDescriptor] = ProvisioningRequestDescriptor(yaml)

    dp.left.value.head should
      startWith("The yaml is not a correct Provisioning Request: cannot parse ComponentIdToProvision for")

  }

  "Parsing a wrongly formed descriptor with missing components section" should "return a Left with a Exception" in {

    val yaml = getTestResourceAsString("pr_descriptors/pr_descriptor_1_missing_components.yml")

    val dp: EitherNel[String, ProvisioningRequestDescriptor] = ProvisioningRequestDescriptor(yaml)

    dp.left.value.head should
      startWith("The yaml is not a correct Provisioning Request: cannot parse Data Product components for")

  }

  "Parsing a wrongly formed descriptor with missing specific section in component" should
    "return a Left with a Exception" in {

      val yaml = getTestResourceAsString("pr_descriptors/pr_descriptor_1_missing_specific.yml")

      val dp: EitherNel[String, ProvisioningRequestDescriptor] = ProvisioningRequestDescriptor(yaml)

      dp.left.value.head should
        startWith("The yaml is not a correct Provisioning Request: cannot parse Component specific for ")

    }

  "Parsing a totally wrongly formed descriptor" should "return a Right with a ParsingFailure" in {

    val yaml = """name: Marketing-Invoice-1
                 |[]
                 |""".stripMargin

    val dp: EitherNel[String, ProvisioningRequestDescriptor] = ProvisioningRequestDescriptor(yaml)

    dp.left.value should be(a[NonEmptyList[_]])
  }

  "Parsing a well formed json component" should "return a correct ComponentDescriptor" in {
    import io.circe._, io.circe.parser._
    val rawHeader: String = """
    {
      "name": "Airflow Workload Test Fix",
      "kind": "workload",
      "infrastructureTemplateId": "urn:dmb:itm:aws-workload-airflow-provisioner:0",
      "useCaseTemplateId": "urn:dmb:utm:aws-airflow-workload-template:0.0.0"
    }
    """
    val rawSpecific       = "{}"

    val component =
      new ComponentDescriptor("", "", parse(rawHeader).getOrElse(Json.Null), parse(rawSpecific).getOrElse(Json.Null))

    component.getName should be(Right("Airflow Workload Test Fix"))
    component.getKind should be(Right("workload"))
    component.getInfrastructureTemplateId should be(Right("urn:dmb:itm:aws-workload-airflow-provisioner:0"))
    component.getUseCaseTemplateId should be(Right(Some("urn:dmb:utm:aws-airflow-workload-template:0.0.0")))
  }

  "Parsing a wrong json component" should "fail" in {
    import io.circe._, io.circe.parser._
    val rawHeader   = "{}"
    val rawSpecific = "{}"

    val component =
      new ComponentDescriptor("", "", parse(rawHeader).getOrElse(Json.Null), parse(rawSpecific).getOrElse(Json.Null))

    component.getName.isLeft should be(true)
    component.getKind.isLeft should be(true)
    component.getInfrastructureTemplateId.isLeft should be(true)
    component.getUseCaseTemplateId.getOrElse(None).isDefined should be(false)
  }

}
