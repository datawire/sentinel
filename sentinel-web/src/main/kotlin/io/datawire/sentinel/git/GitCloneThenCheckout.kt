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

import io.vertx.core.json.JsonObject


data class GitCloneThenCheckout(val cloneUrl: String,
                                val branch: String,
                                val commit: String) {

    companion object {

        private const val JSON_CLONE_URL_FIELD = "git.cloneUrl"
        private const val JSON_BRANCH_FIELD = "git.branch"
        private const val JSON_COMMIT_FIELD = "git.commit"

        @JvmStatic
        fun fromJson(json: JsonObject): GitCloneThenCheckout {
            return GitCloneThenCheckout(
                json.getString(JSON_CLONE_URL_FIELD),
                json.getString(JSON_BRANCH_FIELD),
                json.getString(JSON_COMMIT_FIELD)
            )
        }
    }

    fun toJson(): JsonObject {
        return JsonObject(mapOf(
            JSON_CLONE_URL_FIELD to cloneUrl,
            JSON_BRANCH_FIELD to branch,
            JSON_COMMIT_FIELD to commit
        ))
    }
}