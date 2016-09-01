package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireString
import io.vertx.core.json.JsonObject

data class ServiceSpec(val name: String, val version: String, val public: Boolean) {

  companion object {

    private const val JSON_NAME_FIELD    = "name"
    private const val JSON_VERSION_FIELD = "version"
    private const val JSON_PUBLIC_FIELD  = "public"

    @JvmStatic
    fun fromJson(json: JsonObject): ServiceSpec {
      val name    = json.requireString(JSON_NAME_FIELD)
      val version = json.requireString(JSON_VERSION_FIELD)
      val public  = json.getBoolean(JSON_PUBLIC_FIELD, false)
      return ServiceSpec(name, version, public)
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_NAME_FIELD    to name,
        JSON_VERSION_FIELD to version,
        JSON_PUBLIC_FIELD  to public
    ))
  }
}