package io.datawire.sentinel

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import mdk.Functions
import mdk.MDK


class SentinelServiceVerticle : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(SentinelServiceVerticle::class.java)

  private lateinit var mdk: MDK

  override fun start(startFuture: Future<Void>?) {
    mdk = Functions.init()
    mdk.start()
    logger.info("Datawire MDK started")

    Runtime.getRuntime().addShutdownHook(object : Thread() {
      override fun run() {
        mdk.stop()
        logger.info("Datawire MDK stopped")
      }
    })

    super.start(startFuture)
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