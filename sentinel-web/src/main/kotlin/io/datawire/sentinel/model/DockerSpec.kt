package io.datawire.sentinel.model

import io.vertx.core.json.JsonObject


data class DockerSpec(val registryAddress: String) {

  constructor(): this(DEFAULT_DOCKER_REGISTRY)

  companion object {

    private const val JSON_DOCKER_REGISTRY_ADDRESS_FIELD = "registryAddress"

    private const val DEFAULT_DOCKER_REGISTRY = "docker.io"

    fun fromJson(json: JsonObject): DockerSpec {
      return DockerSpec(
          json.getString(JSON_DOCKER_REGISTRY_ADDRESS_FIELD, DEFAULT_DOCKER_REGISTRY)
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        "registryAddress" to registryAddress
    ))
  }
}