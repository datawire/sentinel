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

package io.datawire.sentinel.util

import com.jcabi.github.*

// Just a tiny temporary little tool for tweaking status on repositories;

fun main(args: Array<String>) {
  val github = RtGithub(System.getProperty("githubToken"))
  val repoCoordinates = Coordinates.Simple("datawire/hello-mobius")

  val repo = github.repos().get(repoCoordinates)

  val newStatus = Statuses.StatusCreate(Status.State.SUCCESS)
      .withDescription("success")

  repo.git().commits().statuses("54fd6595adc87c75c078ab5b78336070d6b1d36b").create(newStatus)
}