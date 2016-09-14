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

import io.datawire.sentinel.model.Organization
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory


class KubeClusterResolver : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(KubeClusterResolver::class.java)

  companion object {
    const val CLUSTER_INFO_ADDR = "kube.organization:cluster"
  }

  override fun start() {
    val clusterInfoHandler = vertx.eventBus().localConsumer<JsonObject>(CLUSTER_INFO_ADDR)
    clusterInfoHandler.handler { msg ->
      val org = Organization(msg.body())
      logger.info("Resolving cluster information for organization (org-id: {}, org-name: {})", org.id, org.name)
      msg.reply(KubeClusterInfo("main", "not-used-yet", "not-used-yet", "not-used-yet"))
    }
  }
}