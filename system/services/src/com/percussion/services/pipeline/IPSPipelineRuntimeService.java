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

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;

/**
 * Execute a saved (or in-memory) pipeline IR resource against a SQL adapter with JSON-oriented
 * request/response.
 *
 * <p>Clean IR boundary for Slice A runtime — does <strong>not</strong> call classic {@code
 * PSQueryHandler} / {@code PSUpdateHandler}.
 */
public interface IPSPipelineRuntimeService {

  /**
   * Load native IR by application name and execute the named resource.
   *
   * @param appName native IR application name
   * @param resourceName resource within the document
   * @param request JSON request values (params / rows); never {@code null}
   */
  PipelineExecuteResult execute(String appName, String resourceName, PipelineExecuteRequest request)
      throws PSPipelineIrException;

  /**
   * Execute a resource from an already-loaded IR document (tests / in-process callers).
   *
   * @param document never {@code null}
   * @param resource never {@code null}; must belong to {@code document}
   * @param request never {@code null}
   */
  PipelineExecuteResult execute(
      PipelineIrDocument document, PipelineResourceIr resource, PipelineExecuteRequest request)
      throws PSPipelineIrException;
}
