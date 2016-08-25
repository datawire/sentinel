package io.datawire.sentinel.kube

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory


class KubeDeploymentHandler(private val vertx: Vertx,
                            private val config: KubeDeployConfig) : Handler<Future<Void>> {

  private val logger = LoggerFactory.getLogger(KubeDeploymentHandler::class.java)

  override fun handle(event: Future<Void>?) {
    logger.info("Running Kubernetes deployment (tenant: {}, image: {})", config.tenant, config.image)


  }
}