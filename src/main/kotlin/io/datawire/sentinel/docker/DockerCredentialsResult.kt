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

import io.datawire.json.requireObject
import io.datawire.json.requireString
import io.datawire.sentinel.model.Organization
import io.vertx.core.json.JsonObject

data class DockerCredentialsResult(val organization: Organization,
                                   val registryAddress: String,
                                   val username: String,
                                   val email: String,
                                   val password: String) {

  constructor(json: JsonObject) : this(
      Organization(json.requireObject(JSON_ORG_FIELD)),
      json.requireString(JSON_REGISTRY_ADDRESS_FIELD),
      json.requireString(JSON_USERNAME_FIELD),
      json.requireString(JSON_PASSWORD_FIELD),
      json.requireString(JSON_EMAIL_FIELD)
  )

  companion object {
    private const val JSON_ORG_FIELD              = "organization"
    private const val JSON_REGISTRY_ADDRESS_FIELD = "registryAddress"
    private const val JSON_USERNAME_FIELD         = "username"
    private const val JSON_PASSWORD_FIELD         = "password"
    private const val JSON_EMAIL_FIELD            = "email"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_ORG_FIELD to organization.toJson(),
      JSON_REGISTRY_ADDRESS_FIELD to registryAddress,
      JSON_USERNAME_FIELD to username,
      JSON_PASSWORD_FIELD to password,
      JSON_EMAIL_FIELD to email
  ))
}