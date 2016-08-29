package io.datawire.vertx

import io.vertx.core.json.JsonObject


abstract class ServiceConfig(val json: JsonObject) {

  constructor(raw: String): this(JsonObject(raw))

  protected fun splitPath(path: String): List<String> {
    return path.split(".")
  }


}