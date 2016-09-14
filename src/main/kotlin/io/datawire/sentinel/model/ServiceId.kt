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


data class ServiceId(val name: String, val version: String) {

  constructor(json: JsonObject) : this(json.requireString(JSON_NAME_FIELD), json.requireString(JSON_VERSION_FIELD))

  val slug = "$name-$version"
  val dockerTag = "$name:$version"

  companion object {
    private const val JSON_NAME_FIELD    = "name"
    private const val JSON_VERSION_FIELD = "version"
  }

  fun toJson() = JsonObject(mapOf(JSON_NAME_FIELD to name, JSON_VERSION_FIELD to version))
}