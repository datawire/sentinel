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
import io.datawire.sentinel.model.Organization
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.extensions.*


data class KubeModels(private val organization: Organization) {

  private fun newMetadataBuilder() = ObjectMetaBuilder().withNamespace(organization.id)

  fun newNamespace(): Namespace {
    val nsMetadata = ObjectMetaBuilder()
        .withName(organization.id)
        .build()

    return NamespaceBuilder().withMetadata(nsMetadata).build()
  }

  fun newService(request: DeploymentRequest): Service {
    val builder = ServiceBuilder()
        .withApiVersion("v1")
        .withMetadata(newMetadataBuilder().withName(request.service.name).build())
        .withSpec(
            ServiceSpecBuilder()
                .withType("LoadBalancer")
                .withSelector(mapOf(
                    "service" to request.service.name
                ))
                .withPorts(
                    ServicePortBuilder()
                        .withName("http")
                        .withPort(80)
                        .withNewTargetPort(5000)
                        .withProtocol("TCP")
                        .build()
                )
                .build()
        )
    return builder.build()
  }

  private fun newPodTemplateSpec(request: DeploymentRequest): PodTemplateSpec {
    val builder = PodTemplateSpecBuilder()
        .withNewMetadata()
          .withLabels(mapOf(
              "service" to request.service.slug
          ))
        .endMetadata()
        .withSpec(
            PodSpecBuilder()
                .withContainers(newContainer(request))
                .build()
        )

    return builder.build()
  }

  private fun newDeploymentSpec(request: DeploymentRequest): DeploymentSpec {
    val builder = DeploymentSpecBuilder()
        .withTemplate(newPodTemplateSpec(request))
        .withPaused(false)
        .withReplicas(request.replicas)

    return builder.build()
  }

  private fun toEnvVars(map: Map<String, String>) {
    map.entries.map { entry -> EnvVar(entry.key, entry.value, null) }
  }

  private fun literalEnvVar(varName: String, varValue: String) = EnvVar(varName, varValue, null)

  private fun injectedSecretEnvVar(name: String, secret: String, secretEntry: String): EnvVar {
    return EnvVar(name, null, EnvVarSourceBuilder().withSecretKeyRef(SecretKeySelector(secret, secretEntry)).build())
  }

  private fun injectedIntrospectionEnvVar(name: String, path: String): EnvVar {
    return EnvVar(name, null, EnvVarSourceBuilder().withFieldRef(ObjectFieldSelector(null, path)).build())
  }

  private fun newContainer(request: DeploymentRequest): Container {
    val builder = ContainerBuilder()
        .withName(request.service.slug)
        .withImage(request.image)
        .withEnv(
            literalEnvVar("MDK_SERVICE_NAME", request.service.name),
            literalEnvVar("MDK_SERVICE_VERSION", request.service.version),
            injectedSecretEnvVar("DATAWIRE_TOKEN", "token", organization.id),
            injectedIntrospectionEnvVar("DATAWIRE_ROUTABLE_HOST", "status.podIP"),
            literalEnvVar("DATAWIRE_ROUTABLE_PORT", "5000")
        )

    return builder.build()
  }

  fun newDeployment(request: DeploymentRequest): Deployment {
    val metadata = newMetadataBuilder().build()

    val deployment = DeploymentBuilder()
        .withMetadata(metadata)
        .withSpec(newDeploymentSpec(request))

    return deployment.build()
  }
}