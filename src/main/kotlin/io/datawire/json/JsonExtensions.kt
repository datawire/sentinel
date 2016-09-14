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

package io.datawire.json

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

private fun missingFieldMessage(field: String) = "JSON field ($field) is null or missing"

fun JsonObject.requireString(field: String): String {
  return this.getString(field) ?: throw IllegalStateException(missingFieldMessage(field))
}

fun JsonObject.requireInteger(field: String): Int {
  return this.getInteger(field) ?: throw IllegalStateException(missingFieldMessage(field))
}

fun JsonObject.requireBoolean(field: String): Boolean {
  return this.getBoolean(field) ?: throw IllegalStateException(missingFieldMessage(field))
}

fun JsonObject.requireObject(field: String): JsonObject {
  return this.getJsonObject(field) ?: throw IllegalStateException(missingFieldMessage(field))
}

fun JsonObject.requireArray(field: String): JsonArray {
  return this.getJsonArray(field) ?: throw IllegalStateException(missingFieldMessage(field))
}