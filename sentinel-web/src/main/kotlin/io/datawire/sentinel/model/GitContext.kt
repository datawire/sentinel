package io.datawire.sentinel.model

import io.datawire.sentinel.json.requireString
import io.vertx.core.json.JsonObject
import java.nio.file.Path
import java.nio.file.Paths


data class GitContext(val repositoryName: String,
                      val repositoryUrl: String,
                      val repositoryCloneUrl: String,
                      val branch: String,
                      val commit: String,
                      val repositoryClonePath: Path) {

  companion object {

    private const val JSON_REPO_CLONE_URL_FIELD  = "repositoryCloneUrl"
    private const val JSON_REPO_NAME_FIELD       = "repositoryName"
    private const val JSON_REPO_URL_FIELD        = "repositoryUrl"
    private const val JSON_BRANCH_FIELD          = "branch"
    private const val JSON_COMMIT_FIELD          = "commit"
    private const val JSON_REPO_CLONE_PATH_FIELD = "repositoryClonePath"

    fun fromJson(json: JsonObject): GitContext {
      return GitContext(
          json.requireString(JSON_REPO_NAME_FIELD),
          json.requireString(JSON_REPO_URL_FIELD),
          json.requireString(JSON_REPO_CLONE_URL_FIELD),
          json.requireString(JSON_BRANCH_FIELD),
          json.requireString(JSON_COMMIT_FIELD),
          Paths.get(json.requireString(JSON_REPO_CLONE_PATH_FIELD))
      )
    }
  }

  fun toJson(): JsonObject {
    return JsonObject(mapOf(
        JSON_REPO_NAME_FIELD       to repositoryName,
        JSON_REPO_URL_FIELD        to repositoryUrl,
        JSON_REPO_CLONE_URL_FIELD  to repositoryCloneUrl,
        JSON_BRANCH_FIELD          to branch,
        JSON_COMMIT_FIELD          to commit,
        JSON_REPO_CLONE_PATH_FIELD to repositoryClonePath.toString()
    ))
  }
}