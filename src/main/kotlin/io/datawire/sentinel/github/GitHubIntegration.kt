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

import io.datawire.sentinel.scm.SourceCodeManagementConfig
import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

import io.netty.handler.codec.http.HttpResponseStatus.*


class GitHubIntegration : AbstractVerticle() {

  private val integrationPath = "/integrations/github"

  override fun start() {
    val integrationConfig = GitHubIntegrationConfig(config().getJsonObject("github"))
    val scmConfig         = SourceCodeManagementConfig(config().getJsonObject("scm"))

    val api = Router.router(vertx)

    api.post("$integrationPath/webhook")
        .consumes("application/json")
        .handler(GitHubWebHookHandler(vertx, scmConfig, integrationConfig))

    api.get("$integrationPath/health").handler { rc ->
      val resp = rc.response()

      with(resp) {
        statusCode    = OK.code()
        statusMessage = OK.reasonPhrase()
      }

      resp.end()
    }

    val server = vertx.createHttpServer()
    val requestHandler = server.requestHandler { api.accept(it) }

    val host = config().getString("host", "0.0.0.0")
    val port = config().getInteger("port", 5000)
    requestHandler.listen(port, host)
  }
}