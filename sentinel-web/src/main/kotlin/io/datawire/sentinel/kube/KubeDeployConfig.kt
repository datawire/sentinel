package io.datawire.sentinel.kube

import io.vertx.core.json.JsonObject


data class KubeDeployConfig(val tenant: String,
                            val image: String,
                            val mobiusConfig: JsonObject) {

  companion object {

    private const val JSON_TENANT_FIELD          = "tenant"
    private const val JSON_DOCKER_IMAGE_ID_FIELD = "docker.image"
    private const val JSON_MOBIUS_DATA_FIELD     = "mobius"

    @JvmStatic fun fromJson(json: JsonObject): KubeDeployConfig {
      return KubeDeployConfig(json.getString(JSON_TENANT_FIELD),
                              json.getString(JSON_DOCKER_IMAGE_ID_FIELD),
                              json.getJsonObject(JSON_MOBIUS_DATA_FIELD))
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_TENANT_FIELD to tenant,
        JSON_DOCKER_IMAGE_ID_FIELD to image,
        JSON_MOBIUS_DATA_FIELD to mobiusConfig
    ))
  }
}