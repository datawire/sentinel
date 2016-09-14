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

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router


class SentinelApiVerticle : AbstractVerticle() {

  override fun start() {
    val api = Router.router(vertx)

    val dockerCreds = vertx.sharedData().getLocalMap<String, String>("docker-credentials")

    val putDockerCredential = api
        .put("/dev/credentials/docker/:organizationId")
        .produces("text/plain")

    putDockerCredential.handler { rc ->
      val orgId  = rc.request().getParam("organizationId")
      val secret = rc.request().getParam("secret")

      if (orgId == null || secret == null) {
        rc.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
      }

      dockerCreds.put(orgId, secret)
      rc.response().setStatusCode(HttpResponseStatus.OK.code()).end("OK")
    }

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { api.accept(it) }

    val host = config().getString("host", "0.0.0.0")
    val port = config().getInteger("port", 5000)
    requestHandler.listen(port, host)
  }
}