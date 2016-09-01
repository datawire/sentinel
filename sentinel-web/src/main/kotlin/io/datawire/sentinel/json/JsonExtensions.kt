package io.datawire.sentinel.json

import io.vertx.core.json.JsonObject

private fun missingFieldMessage(field: String) = "JSON field ($field) is null or missing"

fun JsonObject.requireString(field: String): String {
  return this.getString(field) ?: throw IllegalStateException(missingFieldMessage(field))
}

fun JsonObject.requireObject(field: String): JsonObject {
  return this.getJsonObject(field) ?: throw IllegalStateException(missingFieldMessage(field))
}