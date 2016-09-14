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

package io.datawire.sentinel.kube

import io.datawire.json.requireString
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.vertx.core.json.JsonObject


data class KubeClusterInfo(val profile: String,
                           val masterUrl: String,
                           val adminUsername: String,
                           val adminPassword: String) {

  constructor(json: JsonObject) : this(
      json.getString(JSON_PROFILE_FIELD, "main"),
      json.requireString(JSON_MASTER_URL_FIELD),
      json.requireString(JSON_ADMIN_USERNAME_FIELD),
      json.requireString(JSON_ADMIN_PASSWORD_FIELD)
  )

  companion object {
    private const val JSON_PROFILE_FIELD        = "profile"
    private const val JSON_MASTER_URL_FIELD     = "masterUrl"
    private const val JSON_ADMIN_USERNAME_FIELD = "adminUsername"
    private const val JSON_ADMIN_PASSWORD_FIELD = "adminPassword"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_PROFILE_FIELD to profile,
      JSON_MASTER_URL_FIELD to masterUrl,
      JSON_ADMIN_USERNAME_FIELD to adminUsername,
      JSON_ADMIN_PASSWORD_FIELD to adminPassword
  ))

  fun newKubernetesClient() : KubernetesClient {
    // TODO: Implement proper authorized creation of the KubernetesClient
    //
    // In the future it should be possible for us to resolve the address and credentials for the Kube cluster we
    // want to communicate with (either username + password or certificate should be allowed)
    //
    return DefaultKubernetesClient()
  }
}