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

import io.datawire.sentinel.docker.DockerBuilder
import io.datawire.sentinel.docker.DockerCredentialsVerticle
import io.datawire.sentinel.github.GitHubIntegration
import io.datawire.sentinel.kube.KubeClusterResolver
import io.datawire.sentinel.kube.KubeDeployer
import io.datawire.sentinel.kube.KubeOrganizationInitializer
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import mdk.Functions
import mdk.MDK


class SentinelServiceVerticle : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(SentinelServiceVerticle::class.java)

  private lateinit var mdk: MDK

  override fun start(startFuture: Future<Void>) {
    try {
      mdk = Functions.init()
      mdk.start()
      logger.info("Datawire MDK start succeeded")

      Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
          mdk.stop()
          logger.info("Datawire MDK stopped")
        }
      })

      vertx.deployVerticle(SentinelApiVerticle())

      val workerConfig = DeploymentOptions().setConfig(config()).setWorker(true)

      vertx.deployVerticle(GitHubIntegration(), DeploymentOptions().setConfig(config()))

      vertx.deployVerticle(DockerBuilder(), DeploymentOptions().setConfig(config().getJsonObject("docker")).setWorker(true))
      vertx.deployVerticle(DockerCredentialsVerticle(), workerConfig)

      vertx.deployVerticle(KubeDeployer(), workerConfig)
      vertx.deployVerticle(KubeClusterResolver(), workerConfig)
      vertx.deployVerticle(KubeOrganizationInitializer(), workerConfig)

      startFuture.complete()
    } catch (any: Throwable) {
      logger.error("Datawire MDK start failed", any)
      startFuture.fail(any)
    }
  }

  override fun start() {
    val api = Router.router(vertx)
    api.get("/health").handler {  it.response().setStatusCode(HttpResponseStatus.OK.code()).end() }

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { api.accept(it) }

    val host = config().getString("host", "0.0.0.0")
    val port = config().getInteger("port", 5000)
    requestHandler.listen(port, host)

    registerWithDiscovery()
  }

  private fun registerWithDiscovery() {
    mdk.register("sentinel", "0.1.0", "http://127.0.0.1:5000")
  }
}