package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireObject
import io.vertx.core.json.JsonObject


data class KubernetesDeployContext(val datawire: DatawireContext, val service: ServiceSpec) {

  companion object {

    private const val JSON_DATAWIRE_FIELD = "datawire"
    private const val JSON_SERVICE_FIELD = "service"

    fun fromJson(json: JsonObject): KubernetesDeployContext {
      return KubernetesDeployContext(
          DatawireContext.fromJson(json.requireObject(JSON_DATAWIRE_FIELD)),
          ServiceSpec.fromJson(json.requireObject(JSON_SERVICE_FIELD))
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(
        mapOf(
            "datawire" to datawire.toJson(),
            "service"  to service.toJson()
        )
    )
  }
}