/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.services.pipeline;

import com.percussion.services.pipeline.model.PipelineRequestTrace;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory last-trace store keyed by native pipeline application name. Not a durable log; last
 * execute only. Tests may construct a private instance.
 */
public final class PSPipelineRequestTraceStore {

  private static final PSPipelineRequestTraceStore INSTANCE = new PSPipelineRequestTraceStore();

  private final ConcurrentHashMap<String, PipelineRequestTrace> lastByApp =
      new ConcurrentHashMap<>();

  public static PSPipelineRequestTraceStore getInstance() {
    return INSTANCE;
  }

  public void put(String appName, PipelineRequestTrace trace) {
    if (appName == null || appName.isBlank() || trace == null) {
      return;
    }
    lastByApp.put(appName.trim(), trace);
  }

  public Optional<PipelineRequestTrace> get(String appName) {
    if (appName == null || appName.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(lastByApp.get(appName.trim()));
  }

  public void clear(String appName) {
    if (appName == null || appName.isBlank()) {
      return;
    }
    lastByApp.remove(appName.trim());
  }
}
