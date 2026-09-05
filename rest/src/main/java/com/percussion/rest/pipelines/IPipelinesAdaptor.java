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

package com.percussion.rest.pipelines;

import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import java.net.URI;
import java.util.List;

public interface IPipelinesAdaptor {

  /**
   * List server applications visible to the current security token.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param nameFilter optional case-insensitive name/description substring; blank = no filter
   * @param limit max rows to return (clamped by implementation)
   * @param offset zero-based offset into the sorted result
   * @return application summaries, never {@code null}
   */
  List<ApplicationSummary> listApplications(URI baseUri, String nameFilter, int limit, int offset);

  /**
   * Load one application by internal name or numeric id.
   *
   * @return detail, or {@code null} when not found / not visible
   */
  ApplicationDetail getApplication(URI baseUri, String idOrName);

  /**
   * Load a read-only Pipeline IR document for an application (native IR file, or classic import
   * fallback). Does <strong>not</strong> persist IR or mutate the object store.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param idOrName application numeric id or name
   * @return IR document aligned with pipeline-ir-v1, or {@code null} when not found / not visible
   */
  PipelineIrDocument getPipelineIr(URI baseUri, String idOrName);

  /**
   * Execute a native pipeline IR resource via {@code IPSPipelineRuntimeService}.
   *
   * <p>Developer smoke / Slice A residual — does <strong>not</strong> call classic {@code
   * PSQueryHandler} / {@code PSUpdateHandler}. JSON request/result shapes match the system model
   * types used by the runtime codec.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param appName native IR application name
   * @param resourceName resource within the IR document
   * @param request params/rows body; {@code null} treated as empty by implementations
   * @return execute result, never {@code null}
   */
  PipelineExecuteResult execute(
      URI baseUri, String appName, String resourceName, PipelineExecuteRequest request);

  /**
   * Admin: start a non-hidden classic XML Application / pipeline package (peer {@code
   * PSServer.startApplication}). Idempotent when already running.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param idOrName application numeric id or name
   * @return refreshed detail with {@code active=true}, or {@code null} when not found / not visible
   */
  ApplicationDetail startApplication(URI baseUri, String idOrName);

  /**
   * Admin: stop a non-hidden classic XML Application / pipeline package (peer {@code
   * PSServer.shutdownApplication}). Idempotent when already stopped.
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param idOrName application numeric id or name
   * @return refreshed detail with {@code active=false}, or {@code null} when not found / not visible
   */
  ApplicationDetail stopApplication(URI baseUri, String idOrName);

  /**
   * Admin: run design-time validation / problems for a non-hidden classic XML Application (peer
   * {@code PSValidatorAdapter#validateApplication}).
   *
   * @param baseUri request base URI (reserved for HATEOAS)
   * @param idOrName application numeric id or name
   * @return validation summary, or {@code null} when not found / not visible
   */
  ApplicationValidationResult getValidation(URI baseUri, String idOrName);
}
