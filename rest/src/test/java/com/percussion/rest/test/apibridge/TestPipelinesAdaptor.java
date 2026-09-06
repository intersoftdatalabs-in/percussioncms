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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.pipelines.ApplicationDetail;
import com.percussion.rest.pipelines.ApplicationSummary;
import com.percussion.rest.pipelines.ApplicationValidationResult;
import com.percussion.rest.pipelines.IPipelinesAdaptor;
import com.percussion.rest.pipelines.PipelineHttpBackendTank;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IPipelinesAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code PipelinesResource}.
 */
@Component
@Lazy
public class TestPipelinesAdaptor implements IPipelinesAdaptor {

  @Override
  public List<ApplicationSummary> listApplications(
      URI baseUri, String nameFilter, int limit, int offset) {
    return List.of();
  }

  @Override
  public ApplicationDetail getApplication(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public PipelineIrDocument getPipelineIr(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public Map<String, Object> getOpenApi(URI baseUri, String idOrName) {
    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("openapi", "3.0.3");
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("title", "stub pipeline");
    info.put("version", "1.0");
    spec.put("info", info);
    spec.put("paths", new LinkedHashMap<String, Object>());
    return spec;
  }

  @Override
  public PipelineExecuteResult execute(
      URI baseUri, String appName, String resourceName, PipelineExecuteRequest request) {
    PipelineExecuteResult result = new PipelineExecuteResult();
    result.setAppName(appName);
    result.setResourceName(resourceName);
    result.setOperation("stub");
    result.setRowCount(0);
    result.setAffectedRows(0);
    return result;
  }

  @Override
  public ApplicationDetail startApplication(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public ApplicationDetail stopApplication(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public ApplicationValidationResult getValidation(URI baseUri, String idOrName) {
    return null;
  }

  @Override
  public PipelineHttpBackendTank putHttpBackendTank(
      URI baseUri, String appName, String resourceName, PipelineHttpBackendTank tank) {
    PipelineHttpBackendTank out = tank != null ? tank : new PipelineHttpBackendTank();
    if (out.getAdapterType() == null) {
      out.setAdapterType("HTTP");
    }
    return out;
  }
}
