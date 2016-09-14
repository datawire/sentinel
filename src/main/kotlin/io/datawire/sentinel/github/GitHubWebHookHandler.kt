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

import io.datawire.sentinel.docker.DockerBuilder
import io.datawire.sentinel.exception.ServiceException
import io.datawire.sentinel.git.GitWorkspaceConfig
import io.datawire.sentinel.model.*
import io.datawire.sentinel.scm.SourceCodeManagementConfig
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import kotlinx.support.jdk7.use
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

import kotlin.text.Charsets.UTF_8


class GitHubWebHookHandler(val vertx: Vertx,
                           val scmConfig: SourceCodeManagementConfig,
                           val integrationConfig: GitHubIntegrationConfig) : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(GitHubWebHookHandler::class.java)

  companion object {

    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    private const val GITHUB_WEBHOOK_SIGNATURE_HEADER  = "X-Hub-Signature"
    private const val GITHUB_WEBHOOK_EVENT_DELIVERY_ID = "X-GitHub-Delivery"
    private const val GITHUB_WEBHOOK_EVENT_TYPE_HEADER = "X-GitHub-Event"

    private val PROCESSABLE_EVENTS = setOf("status", "push", "ping")

    private fun isProcessableEvent(type: String) = type.toLowerCase() in PROCESSABLE_EVENTS
    private fun isNotProcessableEvent(type: String) = !isProcessableEvent(type)
  }

  override fun handle(routingContext: RoutingContext?) {

    routingContext?.let { ctx ->
      val request  = ctx.request()
      val response = ctx.response()

      //val eventSignature = request.getHeader(GITHUB_WEBHOOK_SIGNATURE_HEADER)
      val eventId        = request.getHeader(GITHUB_WEBHOOK_EVENT_DELIVERY_ID)
      val eventType      = request.getHeader(GITHUB_WEBHOOK_EVENT_TYPE_HEADER).toLowerCase()

      logger.info("GitHub webhook event received (github-id: {}, type: {})", eventId, eventType)
      if (isNotProcessableEvent(eventType)) {
        logger.error("GitHub webhook event not supported (github-id: {}, type: {})", eventId, eventType)
        response
            .setStatusCode(BAD_REQUEST.code())
            .setStatusMessage(BAD_REQUEST.reasonPhrase())
            .end()

        return@let
      }

      if (eventType == "ping") {
        response.setStatusCode(OK.code()).setStatusMessage(OK.reasonPhrase()).end()
        return@let
      }

      request.bodyHandler { body ->

        val payload = body.toString(UTF_8)
        val json    = JsonObject(payload)

        val repo = json.getJsonObject("repository")
        val (repoOwner, repoName) = repo.getString("full_name").split("/")

        if (repoOwner.toLowerCase() !in integrationConfig.allowedOrganizations) {
          throw ServiceException.UnauthorizedException()
        }

        response.setStatusCode(ACCEPTED.code())
            .setStatusMessage(ACCEPTED.reasonPhrase())
            .end()

        val clonePath = "${scmConfig.workspace}/github/$repoOwner/$repoName"

        val fs = vertx.fileSystem()
        val chain = Future.future<Void>()

        val fut1  = Future.future<Boolean>()
        fs.exists(clonePath, fut1.completer())

        val commit = if (eventType == "status") {
          json.getString("sha")
        } else {
          json.getJsonObject("head_commit").getString("id")
        }

        val workspaceConfig = GitWorkspaceConfig(
            repoName,
            repo.getString("html_url"),
            repo.getString("clone_url"),
            commit,
            Paths.get(clonePath))

        fut1.compose(
            { exists ->
              val res = Future.future<Void>()
              if (exists) {
                fs.deleteRecursive(clonePath, true, res.completer())
                logger.debug("Previous clone directory removed (path: {})", clonePath)
              } else {
                res.complete()
              }
              res
            }
        ).compose(
            { deleted ->
              val res = Future.future<Void>()
              fs.mkdirs(clonePath, res.completer())
              logger.debug("Fresh clone directory created (path: {})", clonePath)
              res
            }
        ).compose(
            { created ->
              val res = Future.future<Void>()

              logger.info("Cloning repository (from: {}, into: {})", workspaceConfig.repositoryCloneUrl, clonePath)
              val clone = Git.cloneRepository()
                  .setURI(workspaceConfig.repositoryCloneUrl)
                  .setDirectory(File(clonePath))

              logger.info("Checking out commit (commit: {})", workspaceConfig.commit)
              clone.call().use { git ->
                git.checkout()
                    .setCreateBranch(true)
                    .setName(workspaceConfig.workBranchName)
                    .setStartPoint(workspaceConfig.commit)
                    .call()
              }

              // brutal hack
              fs.chmod(clonePath + "/entrypoint.sh", "rwxrwxrwx", res.completer())
              res
            }
        ).compose(
            { v ->
              val datawireContext = DatawireContext.load(Paths.get(clonePath))
                  .copy(gitWorkspaceConfig = workspaceConfig)

              logger.info("Sending repository to Docker builder")
              vertx.eventBus().send(DockerBuilder.EVENT_BUS_ADDRESS, datawireContext.toJson())
            }, chain)
      }
    }
  }

  private fun verifySignatures(expected: String, computed: String): Boolean {
    return MessageDigest.isEqual(expected.toByteArray(), computed.toByteArray())
  }

  private fun hmacSha1(secret: String, payload: String): String {
    val signingKey = SecretKeySpec(secret.toByteArray(UTF_8), HMAC_SHA1_ALGORITHM)
    val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM)
    mac.init(signingKey)
    return DatatypeConverter.printHexBinary(mac.doFinal(payload.toByteArray(UTF_8)))
  }
}