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

package io.datawire.sentinel.git

import io.datawire.json.requireString
import io.vertx.core.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*


data class GitWorkspaceConfig(val repositoryName: String,
                              val repositoryUrl: String,
                              val repositoryCloneUrl: String,
                              val commit: String,
                              val repositoryClonePath: Path,
                              val workBranchName: String = UUID.randomUUID().toString()) {

  constructor(json: JsonObject) : this(
      json.requireString(JSON_REPO_NAME_FIELD),
      json.requireString(JSON_REPO_URL_FIELD),
      json.requireString(JSON_REPO_CLONE_URL_FIELD),
      json.requireString(JSON_COMMIT_FIELD),
      Paths.get(json.requireString(JSON_REPO_CLONE_PATH_FIELD)),
      json.getString(JSON_BRANCH_FIELD, UUID.randomUUID().toString())
  )

  companion object {
    private const val JSON_REPO_CLONE_URL_FIELD  = "repositoryCloneUrl"
    private const val JSON_REPO_NAME_FIELD       = "repositoryName"
    private const val JSON_REPO_URL_FIELD        = "repositoryUrl"
    private const val JSON_BRANCH_FIELD          = "workBranch"
    private const val JSON_COMMIT_FIELD          = "commit"
    private const val JSON_REPO_CLONE_PATH_FIELD = "repositoryClonePath"
  }

  fun toJson() = JsonObject(mapOf(
      JSON_REPO_NAME_FIELD       to repositoryName,
      JSON_REPO_URL_FIELD        to repositoryUrl,
      JSON_REPO_CLONE_URL_FIELD  to repositoryCloneUrl,
      JSON_COMMIT_FIELD          to commit,
      JSON_REPO_CLONE_PATH_FIELD to repositoryClonePath.toString(),
      JSON_BRANCH_FIELD          to workBranchName
  ))
}