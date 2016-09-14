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

import com.spotify.docker.client.DefaultDockerClient
import com.spotify.docker.client.ProgressHandler
import io.datawire.sentinel.kube.KubeDeployer
import io.datawire.sentinel.model.DatawireContext
import io.datawire.sentinel.model.DeploymentRequest
import io.datawire.sentinel.model.UpdateStrategy
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.atomic.AtomicReference


class DockerBuilder : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(DockerBuilder::class.java)

  companion object {
    const val EVENT_BUS_ADDRESS = "image-builder.docker"
  }

  override fun start() {
    val dockerConfig = DockerConfig(config())

    val builder = vertx.eventBus().consumer<JsonObject>(EVENT_BUS_ADDRESS)
    builder.handler { msg ->
      val ctx = DatawireContext.fromJson(msg.body())

      val dockerTag = "${dockerConfig.defaultRegistryAddress}/${ctx.service.id.dockerTag}"

      val deployRequest = if (ctx.service.image == null) {
        val docker = DefaultDockerClient.fromEnv().build()
        val imageId = AtomicReference<String>()

        docker.build(ctx.gitWorkspaceConfig?.repositoryClonePath, ProgressHandler { msg ->
          logger.info(msg)
          msg.buildImageId()?.let { id -> imageId.set(id) }
        })

        docker.tag(imageId.get(), dockerTag)

        logger.info("Pushing image (image: {}, tag: {})", imageId.get(), dockerTag)
        docker.push(dockerTag, { msg -> logger.info(msg) })

        DeploymentRequest(ctx.org, ctx.service.id, 1, dockerTag, UpdateStrategy.APPEND, false)
      } else {
        logger.info("Service specification indicates using a pre-built image; Skipping build process (image: {})", ctx.service.image)
        DeploymentRequest(ctx.org, ctx.service.id, 1, ctx.service.image, UpdateStrategy.APPEND, false)
      }

      logger.info("Sending instructions to Kubernetes deployer")
      vertx.eventBus().send(KubeDeployer.VERTX_ADDRESS, deployRequest.toJson())
    }
  }
}