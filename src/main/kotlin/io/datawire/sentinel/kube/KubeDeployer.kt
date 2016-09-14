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

import io.datawire.sentinel.model.DeploymentRequest
import io.datawire.sentinel.model.InitializeOrganizationRequest
import io.datawire.sentinel.model.Organization
import io.datawire.sentinel.model.UpdateStrategy
import io.fabric8.kubernetes.client.KubernetesClient
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory


class KubeDeployer() : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(KubeDeployer::class.java)

  companion object {
    const val VERTX_ADDRESS = "deployer.kubernetes"
    private const val EXECUTE_PARALLEL_FLAG = false
  }

  override fun start() {
    val deploymentEvents = vertx.eventBus().consumer<JsonObject>(VERTX_ADDRESS)
    deploymentEvents.handler { msg ->

      val deployment = DeploymentRequest(msg.body())

      val org      = deployment.organization

      // asynchronously prep the API Gateway if it isn't up and running yet.
      vertx.eventBus().send(KubeOrganizationInitializer.VERTX_ADDRESS, InitializeOrganizationRequest(org).toJson())

      try {
        vertx.eventBus().send<JsonObject>(
            KubeClusterResolver.CLUSTER_INFO_ADDR, org.toJson(),
            { reply ->
              if (reply.succeeded()) {
                val clusterInfo = KubeClusterInfo(reply.result().body())
                val kube = clusterInfo.newKubernetesClient()

                vertx.executeBlocking<Void>(
                    {
                      logger.info("Deploying service (slug: {}, image: {})", deployment.service.slug, deployment.image)
                      this.deploy(kube, deployment)
                    },
                    EXECUTE_PARALLEL_FLAG,
                    { res ->
                      if (res.succeeded()) {
                        logger.info("Deploy service succeeded (slug: {}, replicas: {})")
                      } else {
                        logger.error("Deploy service failed (slug: {}, replicas: {})")
                      }
                    })
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

  private fun deploymentExists(kube: KubernetesClient, organization: Organization, deploymentSlug: String): Boolean {
    val query = kube.extensions().deployments().inNamespace(organization.id).withName(deploymentSlug)
    return query.get() != null
  }

  private fun deploy(kube: KubernetesClient, deployment: DeploymentRequest) {

    // TODO(plombardi, after-v1): Support user-creatable edge services?
    //
    // Should we allow users to deploy an edge service? This is the equivalent of a service such as the API Router
    // that can be accessed directly from the public internet.
    //
    if (deployment.edge) {
      deployEdgeService(kube, deployment)
    }

    // TODO(plombardi, before-v1): This probably needs to be made "more correct" or better defined.
    //
    // Should we allow anything besides append-only services?
    if(!deploymentExists(kube, deployment.organization, deployment.slug)) {
      val models = KubeModels(deployment.organization)
      kube.extensions().deployments().create(models.newDeployment(deployment))
    } else {
      when (deployment.updateStrategy) {
        UpdateStrategy.APPEND  -> deployAppend(kube, deployment)
        UpdateStrategy.ROLLING -> deployRolling(kube, deployment)
        else -> {
          logger.info("Other update strategies not currently support (strategy: {})", deployment.updateStrategy.alias)
        }
      }
    }

    if (!deploymentExists(kube, deployment.organization, deployment.slug)) {
      val models = KubeModels(deployment.organization)
      kube.extensions().deployments().create(models.newDeployment(deployment))


      when (deployment.updateStrategy) {
        UpdateStrategy.APPEND  -> deployAppend(kube, deployment)
        UpdateStrategy.ROLLING -> deployRolling(kube, deployment)
        else -> {
          logger.info("Other update strategies not currently support (strategy: {})", deployment.updateStrategy.alias)
        }
      }
    } else {
      logger.warn("Service is already deployed (org-id: {}, slug: {})",
                  deployment.organization.id, deployment.service.slug)
    }
  }

  private fun deployAppend(kube: KubernetesClient, deployment: DeploymentRequest) {
    val models = KubeModels(deployment.organization)
    kube.extensions().deployments().create(models.newDeployment(deployment))
  }

  private fun deployRolling(kube: KubernetesClient, deployment: DeploymentRequest) {
    val models = KubeModels(deployment.organization)
    kube.extensions().deployments().create(models.newDeployment(deployment))
  }

  private fun deployEdgeService(kube: KubernetesClient, deployment: DeploymentRequest) {
    if (kube.services().inNamespace(deployment.organization.id).withName(deployment.service.name).get() == null) {
      logger.info("Deployment of edge service begun (service: {})", deployment.service.name)
      val models = KubeModels(deployment.organization)
      kube.services().create(models.newService(deployment))
      logger.info("Deployment of edge service finished (service: {})", deployment.service.name)
    }
  }
}