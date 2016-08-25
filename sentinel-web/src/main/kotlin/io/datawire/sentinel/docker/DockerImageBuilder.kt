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
import com.spotify.docker.client.DockerRequestException
import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.AuthConfig
import io.datawire.sentinel.kube.KubeDeployConfig
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.atomic.AtomicReference


class DockerImageBuilder : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(DockerImageBuilder::class.java)

  override fun start() {
    val dockerAuthConfig = AuthConfig.builder()
        .username("_json_key")
        .password(System.getProperty("googleServiceAccountCredential"))
        .email("dev@datawire.io")
        .serverAddress("https://us.gcr.io")
        .build()

    val docker = DefaultDockerClient.fromEnv().authConfig(dockerAuthConfig).build()

    vertx.eventBus().consumer<JsonObject>("docker.image-builder").handler { msg ->
      val instructions = DockerImageBuildConfig.fromJson(msg.body())

      vertx.executeBlocking<Pair<DockerImageBuildConfig, String>>(
          { fut ->
            val imageId = AtomicReference<String>()

            logger.info("Building Docker image (context: {})", instructions.context)
            docker.build(instructions.context, ProgressHandler { msg ->
              logger.info(msg)
              msg.buildImageId()?.let { id -> imageId.set(id) }
            })

            docker.tag(imageId.get(), "us.gcr.io" + "/" + instructions.tag)

            logger.info("Pushing Docker image (image: {})", imageId.get())
            docker.push("us.gcr.io" + "/" + instructions.tag, { msg -> logger.info(msg) })

            fut.complete(Pair(instructions, instructions.tag))
          },
          false,
          { res ->
            if (res.succeeded()) {
              val (dockerImageConfig, dockerTag) = res.result()
              logger.info("Docker image build succeeded (image-id: {})", res.result())

              val kubeConfig = KubeDeployConfig(
                  "datawire",
                  dockerTag,
                  JsonObject(mapOf(
                      "service.name" to dockerImageConfig.serviceName,
                      "service.version" to dockerImageConfig.serviceVersion
                  )))

              vertx.eventBus().send("deployer.kube", kubeConfig.toJson())
            } else {
              logger.error("Docker image build failed", res.cause())
              if (res.cause() is DockerRequestException) {
                logger.error((res.cause() as DockerRequestException).message())
              }
            }
          })
    }
  }
}