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

package io.datawire.sentinel.model


enum class UpdateStrategy(val alias: String) {

  /**
   * Indicates the service should be appended to list of running services whenever an update is required.
   */
  APPEND("append"),

  /**
   * Indicates the service should be destroyed and recreated whenever an update is required.
   */
  RECREATE("recreate"),

  /**
   * Indicates the service should be rolling upgraded whenever an update is required.
   */
  ROLLING("rolling");

  companion object {
    fun fromString(text: String) = when(text.toLowerCase()) {
        APPEND.alias   -> APPEND
        RECREATE.alias -> RECREATE
        ROLLING.alias  -> ROLLING
        else           -> throw IllegalArgumentException("Unknown update strategy (provided: $text)")
      }
  }
}