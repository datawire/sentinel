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

package io.datawire.sentinel.model

import io.datawire.json.requireObject
import io.datawire.json.requireString
import io.datawire.sentinel.git.GitWorkspaceConfig
import io.vertx.core.json.JsonObject
import java.io.InputStream
import java.io.Reader
import java.nio.charset.Charset
import java.nio.file.Files.*
import java.nio.file.Path

data class DatawireContext(val org: Organization, val service: ServiceSpec, val gitWorkspaceConfig: GitWorkspaceConfig?) {

  constructor(json: JsonObject) : this(
      // TODO: This is temporary until we have an internal mechanism to map a repository to an Organization ID and name.
      Organization(json.getJsonObject(JSON_EXT_FIELD).requireString("orgId"),
                   json.getJsonObject(JSON_EXT_FIELD).requireString("orgName")),

      ServiceSpec(json.requireObject(JSON_SERVICE_FIELD)),
      null
  )

  companion object {

    private const val JSON_FORMAT_VERSION_FIELD = "_datawirefileVersion"

    private const val JSON_ORG_FIELD     = "organization"
    private const val JSON_SERVICE_FIELD = "service"
    private const val JSON_EXT_FIELD     = "ext"

    @JvmStatic
    fun load(path: Path, charset: Charset = Charsets.UTF_8): DatawireContext {
      if (isDirectory(path)) {
        for (p in setOf(path.resolve("Datawirefile"), path.resolve("datawirefile"))) {
          if (isRegularFile(p) && isReadable(p)) {
            return load(p)
          }
        }

        throw IllegalStateException("Datawirefile or datawirefile not found in directory '$path'")
      } else {
        return load(newBufferedReader(path, charset))
      }
    }

    @JvmStatic
    fun load(stream: InputStream, charset: Charset = Charsets.UTF_8): DatawireContext {
      return load(stream.bufferedReader(charset))
    }

    @JvmStatic
    fun load(reader: Reader): DatawireContext {
      return reader.use { r -> fromString(r.readText()) }
    }

    @JvmStatic
    fun fromString(text: String): DatawireContext {
      return fromJson(JsonObject(text))
    }

    @JvmStatic
    fun fromJson(json: JsonObject): DatawireContext {
      return DatawireContext(json)
    }
  }

  fun toJson() = JsonObject(mapOf(
      JSON_ORG_FIELD     to org.toJson(),
      JSON_SERVICE_FIELD to service.toJson())
  )
}