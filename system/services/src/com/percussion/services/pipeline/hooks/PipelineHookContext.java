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

package com.percussion.services.pipeline.hooks;

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable context for pre/post execute hooks (spirit of classic request pre-processor / result
 * document processor lifecycle).
 */
public class PipelineHookContext {

  private final PipelineIrDocument document;
  private final PipelineResourceIr resource;
  private final PipelineExecuteRequest request;
  private final List<String> hookTrace = new ArrayList<>();

  public PipelineHookContext(
      PipelineIrDocument document, PipelineResourceIr resource, PipelineExecuteRequest request) {
    this.document = Objects.requireNonNull(document, "document");
    this.resource = Objects.requireNonNull(resource, "resource");
    this.request = Objects.requireNonNull(request, "request");
  }

  public PipelineIrDocument getDocument() {
    return document;
  }

  public PipelineResourceIr getResource() {
    return resource;
  }

  public PipelineExecuteRequest getRequest() {
    return request;
  }

  public List<String> getHookTrace() {
    return hookTrace;
  }

  public void addTrace(String entry) {
    if (entry != null && !entry.isBlank()) {
      hookTrace.add(entry);
    }
  }
}
