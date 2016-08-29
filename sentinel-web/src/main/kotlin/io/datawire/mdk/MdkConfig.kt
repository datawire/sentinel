package io.datawire.mdk

import io.vertx.core.json.JsonObject

/**
 * Utility class to resolve MDK configuration. Default values can be specified in the "mdk" object of the service's
 * configuration file. Overrides are pushed via JVM system properties.
 */


data class MdkConfig(private val config: JsonObject) {

  companion object {
    private val EMPTY_JSON = JsonObject()
  }

  val serviceName    = getConfigString("mdk.service.name")
  val serviceVersion = getConfigString("mdk.service.version")
  val serviceHost    = getConfigString("mdk.service.host")
  val servicePort    = getConfigInt("mdk.service.port")
  val serviceAddress = "$serviceHost:$servicePort"

  private fun parseConfigName(name: String): Triple<String, String, String?> {
    val parts = name.split(".")
    return Triple(parts[0], parts[1], parts[2])
  }

  private fun getConfigString(field: String): String {
    var result = System.getProperty(field)

    val (section, subsection, key) = parseConfigName(field)
    if (result == null) {
      result = config.getJsonObject("mdk", EMPTY_JSON).getJsonObject(subsection, EMPTY_JSON).getString(key)
    }

    return result ?: throw IllegalStateException("No config value for $field")
  }

  private fun getConfigInt(field: String): Int {
    var result = System.getProperty(field)?.toInt()

    val (section, subsection, key) = parseConfigName(field)
    if (result == null) {
      result = config.getJsonObject("mdk", EMPTY_JSON).getJsonObject(subsection, EMPTY_JSON).getInteger(key)
    }

    return result ?: throw IllegalStateException("No config value for $field")
  }
}