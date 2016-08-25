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

package io.datawire.sentinel

import io.datawire.sentinel.deployment.Deployer
import io.datawire.sentinel.docker.DockerImageBuilder
import io.datawire.sentinel.github.GitHubIntegration
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.logging.LoggerFactory


class SentinelService : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(SentinelService::class.java)

  override fun start() {
    vertx.deployVerticle(Deployer(), DeploymentOptions().setWorker(true).setConfig(config())) { deployerStatus ->
      if (deployerStatus.succeeded()) {
        vertx.deployVerticle(DockerImageBuilder(), DeploymentOptions().setWorker(true).setConfig(config())) { builderStatus ->
          if (builderStatus.succeeded()) {
            vertx.deployVerticle(GitHubIntegration(), DeploymentOptions().setConfig(config()))
          } else {
            logger.error("Unable to deploy Builder verticle...")
          }
        }
      } else {
        logger.error("Unable to deploy Deployer verticle...", deployerStatus.cause())
      }
    }
  }
}