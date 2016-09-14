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

import io.datawire.json.requireString
import io.vertx.core.json.JsonObject

data class ServiceSpec(val name: String,
                       val version: String,
                       val image: String?,
                       val public: Boolean,
                       val replicas: Int,
                       val ext: JsonObject) {

  val id  = ServiceId(name, version)

  constructor(json: JsonObject) : this(
      json.requireString(JSON_NAME_FIELD),
      json.requireString(JSON_VERSION_FIELD),
      json.getString(JSON_IMAGE_FIELD),
      json.getBoolean(JSON_PUBLIC_FIELD, true),
      json.getInteger(JSON_REPLICA_FIELD, 1),
      json.getJsonObject(JSON_EXT_FIELD, JsonObject())
  )

  companion object {
    private const val JSON_NAME_FIELD    = "name"
    private const val JSON_VERSION_FIELD = "version"
    private const val JSON_IMAGE_FIELD   = "image"
    private const val JSON_PUBLIC_FIELD  = "public"
    private const val JSON_REPLICA_FIELD = "replicas"
    private const val JSON_EXT_FIELD     = "ext"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_NAME_FIELD    to name,
      JSON_VERSION_FIELD to version,
      JSON_PUBLIC_FIELD  to public,
      JSON_REPLICA_FIELD to replicas,
      JSON_EXT_FIELD     to ext
    ))
}