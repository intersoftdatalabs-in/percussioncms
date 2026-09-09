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
import com.percussion.rest.pipelines.PipelineBinaryResource;
import com.percussion.rest.pipelines.PipelineFilterGroup;
import com.percussion.rest.pipelines.PipelineResultPage;
import com.percussion.rest.pipelines.PipelineHttpBackendTank;
import com.percussion.rest.pipelines.PipelineTracingSettings;
import com.percussion.rest.pipelines.PipelineWebhookHooks;
import com.percussion.services.pipeline.model.PipelineBinaryPayload;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineRequestTrace;
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

  @Override
  public PipelineWebhookHooks putWebhookHooks(
      URI baseUri, String appName, String resourceName, PipelineWebhookHooks hooks) {
    PipelineWebhookHooks out = hooks != null ? hooks : new PipelineWebhookHooks();
    if (out.getHttpMethod() == null) {
      out.setHttpMethod("POST");
    }
    return out;
  }

  @Override
  public PipelineFilterGroup putFilterGroup(
      URI baseUri, String appName, String resourceName, PipelineFilterGroup group) {
    PipelineFilterGroup out = group != null ? group : new PipelineFilterGroup();
    if (out.getType() == null) {
      out.setType("GROUP");
    }
    if (out.getOp() == null) {
      out.setOp("AND");
    }
    return out;
  }

  @Override
  public PipelineBinaryResource putBinaryResource(
      URI baseUri, String appName, String resourceName, PipelineBinaryResource body) {
    PipelineBinaryResource out = body != null ? body : new PipelineBinaryResource();
    if (out.getPath() == null) {
      out.setPath("pipeline-binary-fixture");
    }
    if (out.getContentType() == null) {
      out.setContentType("application/octet-stream");
    }
    return out;
  }

  @Override
  public PipelineResultPage putResultPage(
      URI baseUri, String appName, String resourceName, PipelineResultPage body) {
    PipelineResultPage out = body != null ? body : new PipelineResultPage();
    if (out.isRawPresentation()) {
      out.setPresentation("none");
      return out;
    }
    if (out.getStylesheetUri() == null) {
      out.setStylesheetUri("pipeline-xsl-result-fixture");
    }
    if (out.getRequestExtension() == null) {
      out.setRequestExtension(".html");
    }
    if (out.getMimeType() == null) {
      out.setMimeType("text/html");
    }
    if (out.getPresentation() == null) {
      out.setPresentation("html");
    }
    return out;
  }

  @Override
  public PipelineBinaryPayload retrieveBinary(URI baseUri, String appName, String resourceName) {
    return new PipelineBinaryPayload(
        "text/plain", "PIPE-BIN-FIXTURE\n".getBytes(java.nio.charset.StandardCharsets.UTF_8),
        "pipeline-binary-fixture");
  }

  @Override
  public PipelineTracingSettings putTracing(
      URI baseUri, String idOrName, PipelineTracingSettings settings) {
    PipelineTracingSettings out = settings != null ? settings : new PipelineTracingSettings();
    return out;
  }

  @Override
  public PipelineTracingSettings getTracing(URI baseUri, String idOrName) {
    return new PipelineTracingSettings();
  }

  @Override
  public PipelineRequestTrace getLastTrace(URI baseUri, String idOrName) {
    return null;
  }
}
