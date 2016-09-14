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

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory


class DockerCredentialsVerticle : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(DockerCredentialsVerticle::class.java)

  companion object {
    const val GET_CREDS_ADDR = "docker.credentials:get"
  }

  override fun start() {
    val getCredentials = vertx.eventBus().consumer<JsonObject>(GET_CREDS_ADDR)
    getCredentials.handler { msg ->
      val request = DockerCredentialsRequest(msg.body())
      val credentials = vertx.sharedData().getLocalMap<String, String>("docker-credentials")

      val secret = credentials.get(request.organization.id)
      if (secret != null) {
        logger.info("Retrieve docker credential for organization succeeded (org-id: {})", request.organization.id)
        msg.reply(DockerCredentialsResult(request.organization,
                                          "us.gcr.io",
                                          "_json_key",
                                          "sentinel@datawire.io",
                                          secret))
      } else {
        logger.error("Retrieve docker credential for organization failed (org-id: {})", request.organization.id)
        msg.fail(0, "Docker credential not found for organization (org-id: ${request.organization.id})")
      }
    }
  }
}