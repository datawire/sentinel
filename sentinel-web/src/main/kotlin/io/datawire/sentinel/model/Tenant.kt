package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireString
import io.vertx.core.json.JsonObject


data class Tenant(val id: String, val name: String?) {

  companion object {

    private const val JSON_ID_FIELD   = "id"
    private const val JSON_NAME_FIELD = "name"

    @JvmStatic
    fun fromJson(json: JsonObject): Tenant {
      return Tenant(
          json.requireString(JSON_ID_FIELD),
          json.getString(JSON_NAME_FIELD)
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_ID_FIELD   to id,
        JSON_NAME_FIELD to name
    ))
  }
}