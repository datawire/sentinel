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

package io.datawire.sentinel.github

import io.datawire.quark.runtime.JSONObject
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.Router


class GitHubIntegration : AbstractVerticle() {

  override fun start(startFuture: Future<Void>?) {
    super.start(startFuture)
  }

  override fun start() {
    val router = Router.router(vertx)

    router.post("/integrations/github/webhook")
        .consumes("application/json")
        .handler(GitHubWebHookHandler(vertx, config().getJsonObject("git").getString("workspace")))

    router.get("/integrations/github/health").handler { rc ->
      rc.response().setStatusCode(200).end("OK")
    }

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { router.accept(it) }

    val host = config().getString("host", "0.0.0.0")
    val port = config().getInteger("port", 5000)
    requestHandler.listen(port, host)
  }
}