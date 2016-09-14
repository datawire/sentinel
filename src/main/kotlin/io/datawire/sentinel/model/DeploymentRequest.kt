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
import io.vertx.core.json.JsonObject


data class DeploymentRequest(val organization: Organization,
                             val service: ServiceId,
                             val replicas: Int,
                             val image: String,
                             val updateStrategy: UpdateStrategy,
                             val edge: Boolean = false) {

  val slug = service.slug

  constructor(json: JsonObject) : this(
      Organization(json.requireObject(JSON_ORG_FIELD)),
      ServiceId(json.requireObject(JSON_SERVICE_FIELD)),
      json.getInteger(JSON_REPLICAS_FIELD, 1),
      json.getString(JSON_IMAGE_FIELD, "docker.io"),
      UpdateStrategy.fromString(json.requireString(JSON_UPDATE_STRATEGY_FIELD))
  )

  companion object {
    private const val JSON_ORG_FIELD             = "organization"
    private const val JSON_SERVICE_FIELD         = "service"
    private const val JSON_REPLICAS_FIELD        = "replicas"
    private const val JSON_IMAGE_FIELD           = "image"
    private const val JSON_UPDATE_STRATEGY_FIELD = "updateStrategy"
    private const val JSON_EDGE_SERVICE_FIELD    = "edge"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_ORG_FIELD             to organization.toJson(),
      JSON_SERVICE_FIELD         to service.toJson(),
      JSON_REPLICAS_FIELD        to replicas,
      JSON_IMAGE_FIELD           to image,
      JSON_EDGE_SERVICE_FIELD    to edge,
      JSON_UPDATE_STRATEGY_FIELD to updateStrategy.alias
  ))
}