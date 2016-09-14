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

import io.datawire.json.requireBoolean
import io.datawire.json.requireObject
import io.vertx.core.json.JsonObject


data class InitializeOrganizationResult(val organization: Organization, val exists: Boolean) {

  constructor(json: JsonObject) : this(
      Organization(json.requireObject(JSON_ORG_FIELD)), json.requireBoolean(JSON_ORG_EXISTS_FIELD)
  )

  companion object {
    private const val JSON_ORG_FIELD            = "organization"
    private const val JSON_ORG_EXISTS_FIELD     = "exists"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_ORG_FIELD to organization.toJson(),
      JSON_ORG_EXISTS_FIELD to exists))
}