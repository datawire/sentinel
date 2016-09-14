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

package io.datawire.sentinel.github

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject


class GitHubIntegrationConfig(val allowedOrganizations: Set<String>) {

  constructor(json: JsonObject) : this(
      json.getJsonArray(JSON_ALLOWED_ORGS_FIELD, JsonArray()).mapNotNull { it.toString().toLowerCase() }.toSet()
  )

  companion object {
    private const val JSON_ALLOWED_ORGS_FIELD = "allowedOrganizations"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_ALLOWED_ORGS_FIELD to JsonArray(allowedOrganizations.toList())
  ))
}