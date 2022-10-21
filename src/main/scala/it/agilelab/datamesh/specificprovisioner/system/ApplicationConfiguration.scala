package it.agilelab.datamesh.specificprovisioner.system

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration

object ApplicationConfiguration {

  val config: AtomicReference[Config] = new AtomicReference(ConfigFactory.load())

  def reloadConfig(): String = config.synchronized {
    @tailrec
    def snip(): Unit = {
      val oldConf = config.get
      val newConf = {
        ConfigFactory.invalidateCaches()
        ConfigFactory.load()
      }
      if (!config.compareAndSet(oldConf, newConf)) snip()
    }
    snip()
    config.get.getObject("specific-provisioner").render(ConfigRenderOptions.defaults())
  }

  def httpPort: Int          = config.get.getInt("specific-provisioner.http-port")
  def airflowBaseUrl: String = config.get.getString("airflow-conf.url")
  def user: String           = config.get.getString("airflow-conf.user")
  def password: String       = config.get.getString("airflow-conf.password")

  def functionsInvocationTimeout: FiniteDuration = FiniteDuration(
    config.get().getDuration("specific-provisioner.functions-invocation-timeout", TimeUnit.SECONDS),
    TimeUnit.SECONDS
  )
}
