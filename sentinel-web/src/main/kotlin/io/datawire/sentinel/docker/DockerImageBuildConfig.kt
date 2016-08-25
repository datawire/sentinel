/*
 * Copyright 2016 Datawire. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datawire.sentinel.docker

import io.vertx.core.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths


data class DockerImageBuildConfig(val context: Path,
                                  val registryAddress: String?,
                                  val tenant: String,
                                  val serviceName: String,
                                  val serviceVersion: String) {

  val tag: String
    get() {
      val main = "datawireio/$tenant-$serviceName:$serviceVersion"
      return registryAddress?.let { addr -> "$addr/$main" } ?: main
    }

  companion object {

    private const val JSON_DOCKER_CONTEXT_FIELD  = "docker.contextPath"
    private const val JSON_DOCKER_REGISTRY_FIELD = "docker.registryAddress"
    private const val JSON_DATAWIRE_TENANT       = "datawire.tenant"
    private const val SERVICE_NAME_FIELD         = "service.name"
    private const val SERVICE_VERSION_FIELD      = "service.version"

    @JvmStatic
    fun fromJson(json: JsonObject): DockerImageBuildConfig {
      return DockerImageBuildConfig(
          Paths.get(json.getString(JSON_DOCKER_CONTEXT_FIELD)),
          json.getString(JSON_DOCKER_REGISTRY_FIELD),
          json.getString(JSON_DATAWIRE_TENANT),
          json.getString(SERVICE_NAME_FIELD),
          json.getString(SERVICE_VERSION_FIELD)
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_DOCKER_CONTEXT_FIELD to context.toAbsolutePath().toString(),
        JSON_DOCKER_REGISTRY_FIELD to registryAddress,
        JSON_DATAWIRE_TENANT to tenant,
        SERVICE_NAME_FIELD to serviceName,
        SERVICE_VERSION_FIELD to serviceVersion
    ))
  }
}