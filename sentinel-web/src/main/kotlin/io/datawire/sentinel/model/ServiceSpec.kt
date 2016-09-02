package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireString
import io.vertx.core.json.JsonObject

data class ServiceSpec(val name: String,
                       val version: String,
                       val public: Boolean,
                       val ext: JsonObject) {

  companion object {

    private const val JSON_NAME_FIELD    = "name"
    private const val JSON_VERSION_FIELD = "version"
    private const val JSON_PUBLIC_FIELD  = "public"
    private const val JSON_EXT_FIELD     = "ext"

    @JvmStatic
    fun fromJson(json: JsonObject): ServiceSpec {
      val name    = json.requireString(JSON_NAME_FIELD)
      val version = json.requireString(JSON_VERSION_FIELD)
      val public  = json.getBoolean(JSON_PUBLIC_FIELD, false)
      val ext     = json.getJsonObject(JSON_EXT_FIELD, JsonObject())
      return ServiceSpec(name, version, public, ext)
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_NAME_FIELD    to name,
        JSON_VERSION_FIELD to version,
        JSON_PUBLIC_FIELD  to public,
        JSON_EXT_FIELD     to ext
    ))
  }
}