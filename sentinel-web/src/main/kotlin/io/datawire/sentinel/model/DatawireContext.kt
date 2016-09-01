package io.datawire.sentinel.model

import io.vertx.core.json.JsonObject
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import java.nio.file.Files.*
import java.nio.file.Path

data class DatawireContext(
    val tenant: Tenant,
    val service: ServiceSpec,
    val docker: DockerSpec,
    val git: GitContext?) {

  companion object {

    private const val JSON_FORMAT_VERSION_FIELD = "_datawirefileVersion"
    private const val JSON_SERVICE_FIELD = "service"
    private const val JSON_DOCKER_FIELD = "docker"

    @JvmStatic
    fun load(tenant: Tenant, path: Path, charset: Charset = Charsets.UTF_8): DatawireContext {
      if (isDirectory(path)) {
        for (p in setOf(path.resolve("Datawirefile"), path.resolve("datawirefile"))) {
          if (isRegularFile(p) && isReadable(p)) {
            return load(tenant, p)
          }
        }

        throw IllegalStateException("Datawirefile or datawirefile not found in directory '$path'")
      } else {
        return load(tenant, newBufferedReader(path, charset))
      }
    }

    @JvmStatic
    fun load(tenant: Tenant, stream: InputStream, charset: Charset = Charsets.UTF_8): DatawireContext {
      return load(tenant, stream.bufferedReader(charset))
    }

    @JvmStatic
    fun load(tenant: Tenant, reader: Reader): DatawireContext {
      return reader.use { r -> fromString(tenant, r.readText()) }
    }

    @JvmStatic
    fun fromString(tenant: Tenant, text: String): DatawireContext {
      return fromJson(tenant, JsonObject(text))
    }

    @JvmStatic
    fun fromJson(json: JsonObject): DatawireContext {
      val tenantJson = json.getJsonObject("tenant")
      val tenant = Tenant.fromJson(tenantJson)

      return fromJson(tenant, json)
    }

    @JvmStatic
    fun fromJson(tenant: Tenant, json: JsonObject): DatawireContext {
      val dockerJson = json.getJsonObject("docker", JsonObject())
      val dockerSpec = DockerSpec.fromJson(dockerJson)

      val serviceJson = json.getJsonObject(JSON_SERVICE_FIELD)
      val serviceSpec = serviceJson?.let { ServiceSpec.fromJson(it) } ?: throw IllegalStateException()

      return DatawireContext(tenant, serviceSpec, dockerSpec, null)
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        "tenant"  to tenant.toJson(),
        "service" to service.toJson()
    ))
  }
}