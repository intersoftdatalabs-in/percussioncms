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

import com.percussion.services.pipeline.hooks.IPSPipelinePostExecuteHook;
import com.percussion.services.pipeline.hooks.IPSPipelinePreExecuteHook;
import com.percussion.services.pipeline.hooks.PipelineHookContext;
import com.percussion.services.pipeline.http.IPSPipelineHttpAdapter;
import com.percussion.services.pipeline.http.PSPipelineHttpAdapter;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlanner;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link IPSPipelineRuntimeService}: load IR → pre hooks → HTTP or SQL adapter → post
 * hooks → JSON result.
 */
public class PSPipelineRuntimeService implements IPSPipelineRuntimeService {

  private final IPSPipelineIrService irService;
  private final IPSPipelineSqlAdapter sqlAdapter;
  private final IPSPipelineHttpAdapter httpAdapter;
  private final List<IPSPipelinePreExecuteHook> preHooks;
  private final List<IPSPipelinePostExecuteHook> postHooks;

  public PSPipelineRuntimeService(IPSPipelineIrService irService, IPSPipelineSqlAdapter sqlAdapter) {
    this(irService, sqlAdapter, new PSPipelineHttpAdapter(), List.of(), List.of());
  }

  public PSPipelineRuntimeService(
      IPSPipelineIrService irService,
      IPSPipelineSqlAdapter sqlAdapter,
      List<IPSPipelinePreExecuteHook> preHooks,
      List<IPSPipelinePostExecuteHook> postHooks) {
    this(irService, sqlAdapter, new PSPipelineHttpAdapter(), preHooks, postHooks);
  }

  public PSPipelineRuntimeService(
      IPSPipelineIrService irService,
      IPSPipelineSqlAdapter sqlAdapter,
      IPSPipelineHttpAdapter httpAdapter,
      List<IPSPipelinePreExecuteHook> preHooks,
      List<IPSPipelinePostExecuteHook> postHooks) {
    this.irService = Objects.requireNonNull(irService, "irService");
    this.sqlAdapter = Objects.requireNonNull(sqlAdapter, "sqlAdapter");
    this.httpAdapter = httpAdapter != null ? httpAdapter : new PSPipelineHttpAdapter();
    this.preHooks = preHooks != null ? List.copyOf(preHooks) : List.of();
    this.postHooks = postHooks != null ? List.copyOf(postHooks) : List.of();
  }

  @Override
  public PipelineExecuteResult execute(
      String appName, String resourceName, PipelineExecuteRequest request)
      throws PSPipelineIrException {
    Objects.requireNonNull(appName, "appName");
    Objects.requireNonNull(resourceName, "resourceName");
    Optional<PipelineIrDocument> loaded = irService.load(appName);
    if (loaded.isEmpty()) {
      throw new PSPipelineIrException("Pipeline IR not found: " + appName);
    }
    PipelineIrDocument doc = loaded.get();
    PipelineResourceIr resource = doc.findResource(resourceName);
    if (resource == null) {
      throw new PSPipelineIrException(
          "Resource not found in IR " + appName + ": " + resourceName);
    }
    return execute(doc, resource, request);
  }

  @Override
  public PipelineExecuteResult execute(
      PipelineIrDocument document, PipelineResourceIr resource, PipelineExecuteRequest request)
      throws PSPipelineIrException {
    Objects.requireNonNull(document, "document");
    Objects.requireNonNull(resource, "resource");
    // Interface contract: resource must belong to document (same instance from findResource).
    String resourceName = resource.getName();
    if (resourceName == null || document.findResource(resourceName) != resource) {
      throw new PSPipelineIrException(
          "Resource does not belong to the provided pipeline document"
              + (resourceName != null ? ": " + resourceName : ""));
    }
    PipelineExecuteRequest req = request != null ? request : PipelineExecuteRequest.empty();

    PipelineHookContext ctx = new PipelineHookContext(document, resource, req);
    for (IPSPipelinePreExecuteHook hook : preHooks) {
      hook.beforeExecute(ctx);
    }

    PipelineExecuteResult result = new PipelineExecuteResult();
    String appName = document.getApp() != null ? document.getApp().getName() : null;
    result.setAppName(appName);
    result.setResourceName(resource.getName());
    result.setKind(resource.getKind());

    BackendTankStageIr tank =
        resource.getStages() != null ? resource.getStages().getBackendTank() : null;
    if (tank != null && tank.isHttpAdapter()) {
      if (!PipelineResourceIr.KIND_QUERY.equals(resource.getKind())
          && resource.getKind() != null
          && !PipelineResourceIr.KIND_UNKNOWN.equals(resource.getKind())) {
        throw new PSPipelineIrException(
            "HTTP datasource supports QUERY resources only in this slice");
      }
      List<Map<String, Object>> rows = httpAdapter.query(resource, req);
      result.setOperation("http-query");
      result.setKind(PipelineResourceIr.KIND_QUERY);
      result.setRows(rows);
      result.getMeta().put("adapterType", BackendTankStageIr.ADAPTER_HTTP);
      result.getMeta().put("httpUrl", tank.getUrl());
    } else if (PipelineResourceIr.KIND_QUERY.equals(resource.getKind())) {
      PSPipelineSqlPlan plan = PSPipelineSqlPlanner.planQuery(resource, req);
      List<Map<String, Object>> rows = sqlAdapter.query(plan);
      result.setOperation("query");
      result.setRows(rows);
      result.getMeta().put("sqlDescription", plan.getDescription());
      result.getMeta().put("parameterCount", plan.getParameters().size());
    } else if (PipelineResourceIr.KIND_UPDATE.equals(resource.getKind())) {
      String mutation = PSPipelineSqlPlanner.resolveMutationOperation(resource, req);
      List<PSPipelineSqlPlan> plans;
      if (PipelineExecuteRequest.OP_INSERT.equals(mutation)) {
        plans = PSPipelineSqlPlanner.planInserts(resource, req);
      } else if (PipelineExecuteRequest.OP_UPDATE.equals(mutation)) {
        plans = PSPipelineSqlPlanner.planUpdates(resource, req);
      } else if (PipelineExecuteRequest.OP_DELETE.equals(mutation)) {
        plans = PSPipelineSqlPlanner.planDeletes(resource, req);
      } else {
        throw new PSPipelineIrException("Unsupported mutation operation: " + mutation);
      }
      String txMode = resource.getTransactionMode();
      int affected = sqlAdapter.updateAll(plans, txMode);
      result.setOperation(mutation);
      result.setAffectedRows(affected);
      result.setRowCount(0);
      result.getMeta().put("planCount", plans.size());
      result.getMeta().put(
          "transactionMode",
          txMode != null && !txMode.isBlank()
              ? txMode.trim().toLowerCase(Locale.ROOT)
              : "none");
    } else {
      throw new PSPipelineIrException(
          "Unsupported resource kind for runtime execute: " + resource.getKind());
    }

    for (IPSPipelinePostExecuteHook hook : postHooks) {
      hook.afterExecute(ctx, result);
    }
    // Post hooks may append to context trace; publish full ordered trace on the result.
    result.setHookTrace(new ArrayList<>(ctx.getHookTrace()));

    return result;
  }
}
