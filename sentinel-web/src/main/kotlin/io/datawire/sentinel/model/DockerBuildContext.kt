package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireObject
import io.datawire.sentinel.json.requireString
import io.vertx.core.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths


data class DockerBuildContext(val datawire: DatawireContext, val buildContext: Path) {

  val tag = "datawireio/${datawire.tenant.id}-${datawire.service.name}:${datawire.service.version}"

  companion object {

    @JvmStatic
    fun fromJson(json: JsonObject): DockerBuildContext {
      return DockerBuildContext(
          DatawireContext.fromJson(json.requireObject("datawire")),
          Paths.get(json.requireString("buildContext"))
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        "datawire"     to datawire.toJson(),
        "buildContext" to buildContext.toString()
    ))
  }
}