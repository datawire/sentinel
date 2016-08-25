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

import io.datawire.sentinel.docker.DockerImageBuildConfig
import io.datawire.sentinel.exception.ServiceException
import io.datawire.sentinel.git.GitCloneThenCheckout
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.shareddata.AsyncMap
import io.vertx.ext.web.RoutingContext
import kotlinx.support.jdk7.use
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

import kotlin.text.Charsets.UTF_8


class GitHubWebHookHandler(
    val vertx: Vertx,
    val gitWorkspace: String,
    val secrets: AsyncMap<String, String>) : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(GitHubWebHookHandler::class.java)

  companion object {

    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    private const val GITHUB_WEBHOOK_SIGNATURE_HEADER  = "X-Hub-Signature"
    private const val GITHUB_WEBHOOK_EVENT_DELIVERY_ID = "X-GitHub-Delivery"
    private const val GITHUB_WEBHOOK_EVENT_TYPE_HEADER = "X-GitHub-Event"

    private const val NO_EXECUTE_SEQUENTIALLY = false

    private val PROCESSABLE_EVENTS = setOf("status")
  }

  override fun handle(routingContext: RoutingContext?) {
    routingContext?.let { ctx ->
      ctx.request().bodyHandler { body ->
        val response = ctx.response()

        val payload = body.toString(UTF_8)
        val json = JsonObject(payload)
        val repoInfo = json.getJsonObject("repository")
        val (tenant, repoName) = repoInfo.getString("full_name").split("/")
        val expectedSignature = ctx.request().getHeader(GITHUB_WEBHOOK_SIGNATURE_HEADER)

        secrets.get("$tenant/$repoName") { get ->
          if (get.succeeded()) {
            val secret = get.result()
            val computedSignature = "sha1=${hmacSha1(secret, payload)}".toLowerCase()
            if (verifySignatures(expectedSignature, computedSignature)) {

              response.setStatusCode(ACCEPTED.code())
                  .setStatusMessage(ACCEPTED.reasonPhrase())
                  .end()

              json.getString("state")?.toLowerCase()?.let { status ->
                if (status == "success") {

                  val cloneThenCheckout = GitCloneThenCheckout(
                      repoInfo.getString("clone_url"),
                      "master",
                      json.getString("sha")
                  )

                  vertx.executeBlocking<Pair<Path, JsonObject>>(
                      { fut ->

                        val clonePath = "$gitWorkspace/${repoInfo.getString("name")}"
                        if (vertx.fileSystem().existsBlocking(clonePath)) {
                          vertx.fileSystem().deleteRecursiveBlocking(clonePath, true)
                        }

                        vertx.fileSystem().mkdirsBlocking(clonePath)

                        logger.info("Cloning repository (from: {}, into: {})", cloneThenCheckout.cloneUrl, clonePath)
                        val clone = Git.cloneRepository()
                            .setURI(cloneThenCheckout.cloneUrl)
                            .setDirectory(File(clonePath))

                        clone.call().use { git ->
                          git.checkout()
                              .setName(cloneThenCheckout.branch)
                              .setStartPoint(cloneThenCheckout.commit)
                              .call()
                        }

                        val mobiusfilePath = clonePath + "/Mobiusfile"
                        if (vertx.fileSystem().existsBlocking(mobiusfilePath)) {
                          val mobiusData = JsonObject(File(mobiusfilePath).readText(Charsets.UTF_8))
                          mobiusData.put("tenant", tenant)

                          fut.complete(Pair(Paths.get(clonePath), mobiusData))
                        } else {
                          logger.error("Mobiusfile not found in repository (repo: {})", clonePath)
                          fut.fail(NoSuchFileException(File(mobiusfilePath)))
                        }
                      },
                      NO_EXECUTE_SEQUENTIALLY,
                      { res ->
                        if (res.succeeded()) {
                          logger.info("Repository clone and checkout succeeded")

                          val (contextPath, mobiusData) = res.result()

                          val buildDockerImageMessage = DockerImageBuildConfig(
                              context         = contextPath,
                              registryAddress = null,         // TODO: use ours for the time being
                              tenant          = tenant,       // TODO: this needs to be looked up somehow
                              serviceName     = mobiusData.getJsonObject("service").getString("name"),
                              serviceVersion  = mobiusData.getJsonObject("service").getString("version"))

                          vertx.eventBus().send("docker.image-builder", buildDockerImageMessage.toJson())
                        } else {
                          logger.error("Repository clone and checkout failed", res.cause())
                        }
                      }
                  )
                }
              }
            } else {
              throw ServiceException.UnauthorizedException()
            }
          }
        }
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