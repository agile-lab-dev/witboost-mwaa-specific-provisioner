package it.agilelab.datamesh.mwaaspecificprovisioner.system

import com.typesafe.config.{Config, ConfigFactory}

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration

object ApplicationConfiguration {

  val config: AtomicReference[Config] = new AtomicReference(ConfigFactory.load())

  def httpPort: Int = config.get.getInt("specific-provisioner.http-port")

  def isMock =
    if (config.get.hasPath("specific-provisioner.is-mock")) config.get.getBoolean("specific-provisioner.is-mock")
    else false

  def functionsInvocationTimeout: FiniteDuration = FiniteDuration(
    config.get().getDuration("specific-provisioner.functions-invocation-timeout", TimeUnit.SECONDS),
    TimeUnit.SECONDS
  )
}
