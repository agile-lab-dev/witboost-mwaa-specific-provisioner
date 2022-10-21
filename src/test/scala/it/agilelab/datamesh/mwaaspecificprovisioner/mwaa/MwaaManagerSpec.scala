package it.agilelab.datamesh.mwaaspecificprovisioner.mwaa

import it.agilelab.datamesh.mwaaspecificprovisioner.common.test.getTestResourceAsString
import it.agilelab.datamesh.specificprovisioner.model.ProvisioningRequestDescriptor
import it.agilelab.datamesh.specificprovisioner.mwaa.{GetDagNameError, MwaaManager}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class MwaaManagerSpec extends AnyFlatSpec {
  private val mwaaManager = new MwaaManager

  "executeProvision" should "return an GetDagNameError when dagName is not in the specific field" in {
    val yaml       = getTestResourceAsString("pr_descriptors/pr_descriptor_1_missing_dagName.yaml")
    val descriptor = ProvisioningRequestDescriptor(yaml)

    val result = mwaaManager.executeProvision(descriptor.toOption.get)

    result should matchPattern {
      case Left(GetDagNameError(_, "Attempt to decode value on failed cursor: DownField(dagName)")) => ()
    }
  }

  "executeProvision" should
    "return an GetDagNameError when componentIdToProvision is different from the one contained in components" in {
      val yaml       = getTestResourceAsString("pr_descriptors/pr_descriptor_1_unmatched_component.yaml")
      val descriptor = ProvisioningRequestDescriptor(yaml)
      val result     = mwaaManager.executeProvision(descriptor.toOption.get)

      result should matchPattern { case Left(GetDagNameError(_, "Unable to find the component to provision")) => () }
    }
}
