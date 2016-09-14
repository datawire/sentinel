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

import io.datawire.sentinel.model.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory


class KubeOrganizationInitializer : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(KubeOrganizationInitializer::class.java)

  companion object {
    const val VERTX_ADDRESS = "kube.organization:init"
  }

  override fun start() {
    val initializer = vertx.eventBus().consumer<JsonObject>(VERTX_ADDRESS)
    initializer.handler { msg ->
      try {
        val request = InitializeOrganizationRequest(msg.body())
        val org = request.organization

        vertx.eventBus().send<JsonObject>(
            KubeClusterResolver.CLUSTER_INFO_ADDR, org.toJson(),
            { reply ->
              if (reply.succeeded()) {
                val clusterInfo = KubeClusterInfo(reply.result().body())
                val kube = clusterInfo.newKubernetesClient()

                val created = createOrganizationNamespace(kube, org)
                if (created) {
                  val currentApiGatewayVersion = vertx.sharedData().getLocalMap<String, String>("dev-config")["api-gateway-current-version"]
                  val deployApiGateway         = DeploymentRequest(org, ServiceId("api-gateway", currentApiGatewayVersion), 1, "us.gcr.io", UpdateStrategy.ROLLING, true)
                  vertx.eventBus().send(KubeDeployer.VERTX_ADDRESS, deployApiGateway.toJson())
                }

                msg.reply(InitializeOrganizationResult(org, true))
              } else {
                logger.error("Could not find organization's Kubernetes cluster information (org-id: {}, org-name: {})",
                             org.id, org.name)
                msg.fail(0, "Organization namespace not found")
              }
            })
      } catch (any: Throwable) {
        logger.error("Organization setup failed", any)
        msg.fail(0, any.message)
      }
    }
  }

  private fun namespaceExists(kube: KubernetesClient, namespace: String): Boolean {
    return kube.namespaces().withName(namespace).get() != null
  }

  private fun createOrganizationNamespace(kube: KubernetesClient, organization: Organization): Boolean {
    return if (!namespaceExists(kube, organization.id)) {
      logger.info("Namespace for organization to be created (org-id: {})")
      kube.namespaces().create(KubeModels(organization).newNamespace())
      logger.info("Namespace for organization created (org-id: {})", organization.id)
      true
    } else {
      logger.info("Namespace for organization exists already (org-id: {})", organization.id)
      false
    }
  }
}