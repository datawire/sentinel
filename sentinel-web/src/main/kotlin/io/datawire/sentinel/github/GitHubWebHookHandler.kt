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

import io.datawire.sentinel.exception.ServiceException
import io.datawire.sentinel.model.DatawireContext
import io.datawire.sentinel.model.DockerBuildContext
import io.datawire.sentinel.model.GitContext
import io.datawire.sentinel.model.Tenant
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


class GitHubWebHookHandler(
    val vertx: Vertx,
    val gitWorkspace: String) : Handler<RoutingContext> {

  private val logger = LoggerFactory.getLogger(GitHubWebHookHandler::class.java)

  companion object {

    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    private const val GITHUB_WEBHOOK_SIGNATURE_HEADER  = "X-Hub-Signature"
    private const val GITHUB_WEBHOOK_EVENT_DELIVERY_ID = "X-GitHub-Delivery"
    private const val GITHUB_WEBHOOK_EVENT_TYPE_HEADER = "X-GitHub-Event"

    private const val NO_EXECUTE_SEQUENTIALLY = false

    private val PROCESSABLE_EVENTS = setOf("status", "push", "ping") // todo: support push events as well in the future.

    private fun isProcessableEvent(type: String) = type.toLowerCase() in PROCESSABLE_EVENTS
    private fun isNotProcessableEvent(type: String) = !isProcessableEvent(type)
  }

  override fun handle(routingContext: RoutingContext?) {
    routingContext?.let { ctx ->
      val request  = ctx.request()
      val response = ctx.response()

      val eventSignature = request.getHeader(GITHUB_WEBHOOK_SIGNATURE_HEADER)
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
        response.setStatusCode(ACCEPTED.code())
            .setStatusMessage(ACCEPTED.reasonPhrase())
            .end()

        return@let
      }

      request.bodyHandler { body ->

        val payload  = body.toString(UTF_8)
        val json     = JsonObject(payload)

        val repo      = json.getJsonObject("repository")
        val (repoOwner, repoName) = repo.getString("full_name").split("/")

        // TODO: To be multi-tenant we will need to query this data based on repo identity; For now this is OK.
        // All the code using tenancy in the various model objects needs to be updated when this is fixed. Shouldn't be
        // possible to be null (that'd be a non-tenant) etc.
        val tenant = Tenant(repoOwner, repoOwner)

        if (tenant.id != "datawire") {
          throw ServiceException.UnauthorizedException()
        }

        response.setStatusCode(ACCEPTED.code())
            .setStatusMessage(ACCEPTED.reasonPhrase())
            .end()

        val clonePath = "$gitWorkspace/$repoOwner/$repoName"

        val fs    = vertx.fileSystem()
        val chain = Future.future<Void>()

        val fut1  = Future.future<Boolean>()
        fs.exists(clonePath, fut1.completer())

        val commit = if (eventType == "status") {
          json.getString("sha")
        } else {
          json.getJsonObject("head_commit").getString("id")
        }

        val gitContext = GitContext(
            repoName,
            repo.getString("html_url"),
            repo.getString("clone_url"),
            "sentinel",
            commit,
            Paths.get(clonePath))

        fut1.compose(
            { exists ->
              val res = Future.future<Void>()
              if (exists) {
                fs.deleteRecursive(clonePath, true, res.completer())
                logger.debug("Deleted previous clone directory path (path: {})", clonePath)
              } else {
                res.complete()
              }
              res
            }
        ).compose(
            { deleted ->
              val res = Future.future<Void>()
              fs.mkdirs(clonePath, res.completer())
              logger.debug("Created new clone directory path (path: {})", clonePath)
              res
            }
        ).compose(
            { created ->
              val res = Future.future<Void>()

              logger.info("Cloning repository (from: {}, into: {})", gitContext.repositoryCloneUrl, clonePath)
              val clone = Git.cloneRepository()
                  .setURI(gitContext.repositoryCloneUrl)
                  .setDirectory(File(clonePath))

              logger.info("Checking out commit (commit: {})", gitContext.commit)
              clone.call().use { git ->
                git.checkout()
                    .setCreateBranch(true)
                    .setName(gitContext.branch)
                    .setStartPoint(gitContext.commit)
                    .call()
              }

              // brutal hack
              fs.chmodBlocking(clonePath + "/entrypoint.sh", "rwxrwxrwx")

              res.complete()
              res
            }).compose(
                { v ->
                  val datawireContext = DatawireContext
                      .load(tenant, gitContext.repositoryClonePath, Charsets.UTF_8)
                      .copy(git = gitContext)

                  val dockerBuildContext = DockerBuildContext(
                      datawireContext,
                      datawireContext.git!!.repositoryClonePath)

                  logger.info("Sending repository to Docker builder")
                  vertx.eventBus().send("docker.image-builder", dockerBuildContext.toJson())
                }, chain)


//        gitClone.compose(Function {
//          val
//
//        })
//
//
//
//        fs.createFile("$gitWorkspace/$repoOwner/$repoName", chain.completer())
//        chain.compose<Void>({a: Void, b: Future<Void> -> })
//
//
//        chain.compose<Void, Void>({}, {})
//
//
//        createClonePath()

//        val gitContext = GitContext(repoName,
//                                    repo.getString("html_url"),
//                                    repo.getString("clone_url"),
//                                    "",
//                                    "",
//
//                                    )



//        // TODO: needs improvement to support Push events (different JSON structure)
//        json.getString("state")?.to                  logger.info("Cloning repository (from: {}, into: {})", gitContext.repositoryCloneUrl, clonePath)
//LowerCase()?.let { status ->
//          if (status == "success") {
//
////            val clonePath = "$gitWorkspace/$tenantName/$repoName}"
////            val gitContext = GitContext(
////                repoInfo.getString("full_name"),
////                repoInfo.getString("html_url"),
////                repoInfo.getString("clone_url"),
////                "unknown-branch", // apparently the branch isn't transmitted with the Status event?!
////                json.getString("sha"),
////                Paths.get(clonePath)
////            )
//
//            vertx.executeBlocking<DatawireContext>(
//                { fut ->
//
//                  if (vertx.fileSystem().existsBlocking(clonePath)) {
//                    vertx.fileSystem().deleteRecursiveBlocking(clonePath, true)
//                  }
//
//                  vertx.fileSystem().mkdirsBlocking(clonePath)
//
//                  logger.info("Cloning repository (from: {}, into: {})", gitContext.repositoryCloneUrl, clonePath)
//                  val clone = Git.cloneRepository()
//                      .setURI(gitContext.repositoryCloneUrl)
//                      .setDirectory(File(clonePath))
//
//                  logger.info("Checking out commit (commit: {})", gitContext.commit)
//                  clone.call().use { git ->
//                    git.checkout()
//                        .setCreateBranch(true)
//                        .setName("sentinel-build")
//                        .setStartPoint(gitContext.commit)
//                        .call()
//                  }
//
//                  val datawireContext = DatawireContext
//                      .load(tenant, gitContext.repositoryClonePath, Charsets.UTF_8)
//                      .copy(git = gitContext)
//
//                  fut.complete(datawireContext)
//                },
//                NO_EXECUTE_SEQUENTIALLY,
//                { res ->
//                  if (res.succeeded()) {
//                    val datawireContext = res.result()
//
//                    logger.info("Repository clone and checkout succeeded (tenant: {})", datawireContext.tenant.id)
//
//                    val dockerBuildContext = DockerBuildContext(
//                        datawireContext,
//                        datawireContext.git!!.repositoryClonePath) // todo: this probably shouldn't need to be a !!
//
//                    vertx.eventBus().send("docker.image-builder", dockerBuildContext.toJson())
//                  } else {
//                    logger.error("Repository clone and checkout failed", res.cause())
//                  }
//                }
//            )
//          }
//        }

//        secrets.get("${tenant.id}/$repoName") { get ->
//          if (get.succeeded()) {
//            val secret = get.result()
//            val computedSignature = "sha1=${hmacSha1(secret, payload)}".toLowerCase()
//            if (verifySignatures(eventSignature, computedSignature)) {
//
//              response.setStatusCode(ACCEPTED.code())
//                  .setStatusMessage(ACCEPTED.reasonPhrase())
//                  .end()
//
//              // TODO: needs improvement to support Push events (different JSON structure)
//              json.getString("state")?.toLowerCase()?.let { status ->
//                if (status == "success") {
//
//                  val clonePath = "$gitWorkspace/$tenantName/$repoName}"
//                  val gitContext = GitContext(
//                      repoInfo.getString("full_name"),
//                      repoInfo.getString("html_url"),
//                      repoInfo.getString("clone_url"),
//                      "unknown-branch", // apparently the branch isn't transmitted with the Status event?!
//                      json.getString("sha"),
//                      Paths.get(clonePath)
//                  )
//
//                  vertx.executeBlocking<DatawireContext>(
//                      { fut ->
//
//                        if (vertx.fileSystem().existsBlocking(clonePath)) {
//                          vertx.fileSystem().deleteRecursiveBlocking(clonePath, true)
//                        }
//
//                        vertx.fileSystem().mkdirsBlocking(clonePath)
//
//                        logger.info("Cloning repository (from: {}, into: {})", gitContext.repositoryCloneUrl, clonePath)
//                        val clone = Git.cloneRepository()
//                            .setURI(gitContext.repositoryCloneUrl)
//                            .setDirectory(File(clonePath))
//
//                        logger.info("Checking out commit (commit: {})", gitContext.commit)
//                        clone.call().use { git ->
//                          git.checkout()
//                              .setName("sentinel-build")
//                              .setStartPoint(gitContext.commit)
//                              .call()
//                        }
//
//                        val datawireContext = DatawireContext
//                            .load(tenant, gitContext.repositoryClonePath, Charsets.UTF_8)
//                            .copy(git = gitContext)
//
//                        fut.complete(datawireContext)
//                      },
//                      NO_EXECUTE_SEQUENTIALLY,
//                      { res ->
//                        if (res.succeeded()) {
//                          val datawireContext = res.result()
//
//                          logger.info("Repository clone and checkout succeeded (tenant: {})", datawireContext.tenant.id)
//
//                          val dockerBuildContext = DockerBuildContext(
//                              datawireContext,
//                              datawireContext.git!!.repositoryClonePath) // todo: this probably shouldn't need to be a !!
//
//                          vertx.eventBus().send("docker.image-builder", dockerBuildContext.toJson())
//                        } else {
//                          logger.error("Repository clone and checkout failed", res.cause())
//                        }
//                      }
//                  )
//                }
//              }
//            } else {
//              throw ServiceException.UnauthorizedException()
//            }
//          }
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