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

import io.datawire.mdk.MdkConfig
import io.datawire.sentinel.deployment.Deployer
import io.datawire.sentinel.docker.DockerImageBuilder
import io.datawire.sentinel.github.GitHubIntegration
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.logging.LoggerFactory
import mdk.Functions


class SentinelService : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(SentinelService::class.java)

  private fun initializeMdk() {
    logger.info("Initializing Datawire MDK")
    val mdkConfig = MdkConfig(config())
    val mdk = Functions.init()

    Runtime.getRuntime().addShutdownHook(Thread {
      logger.info("Gracefully stopping Datawire MDK")
      mdk.stop()
    })

    logger.info("Starting Datawire MDK")
    mdk.start()
    mdk.register(mdkConfig.serviceName, mdkConfig.serviceVersion, "http://${mdkConfig.serviceAddress}")
  }

  override fun start() {
    logger.info("Deploy Sentinel...")
    val workerConfig = DeploymentOptions()
        .setConfig(config())
        .setWorker(true)

    vertx.deployVerticle(DockerImageBuilder(), workerConfig)
    vertx.deployVerticle(Deployer(), workerConfig) { deployerDeployment ->
      if (deployerDeployment.succeeded()) {
        vertx.deployVerticle(GitHubIntegration(), DeploymentOptions().setConfig(config())) { githubDeployment ->
          if (githubDeployment.succeeded()) {
            logger.info("GitHub integration start succeeded")
            initializeMdk()
          } else {
            logger.error("GitHub integration start failed", githubDeployment.cause())
          }
        }
      } else {
        logger.error("Deployer component start failed")
      }
    }
  }
}