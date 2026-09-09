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

import com.percussion.services.pipeline.binary.PSPipelineBinaryAdapter;
import com.percussion.services.pipeline.hooks.IPSPipelinePostExecuteHook;
import com.percussion.services.pipeline.hooks.IPSPipelinePreExecuteHook;
import com.percussion.services.pipeline.hooks.PipelineHookContext;
import com.percussion.services.pipeline.hooks.PSPipelineHttpWebhookInvoker;
import com.percussion.services.pipeline.http.IPSPipelineHttpAdapter;
import com.percussion.services.pipeline.http.PSPipelineHttpAdapter;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.FilterGroupIr;
import com.percussion.services.pipeline.model.PipelineBinaryPayload;
import com.percussion.services.pipeline.model.PipelineExecuteRequest;
import com.percussion.services.pipeline.model.PipelineExecuteResult;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineRequestTrace;
import com.percussion.services.pipeline.model.PipelineRequestTraceStage;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineResultPageIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.xsl.PSPipelineXslMerge;
import com.percussion.services.pipeline.sql.IPSPipelineSqlAdapter;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlan;
import com.percussion.services.pipeline.sql.PSPipelineSqlPlanner;
import java.time.Instant;
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
  private final PSPipelineBinaryAdapter binaryAdapter;
  private final List<IPSPipelinePreExecuteHook> preHooks;
  private final List<IPSPipelinePostExecuteHook> postHooks;
  private final PSPipelineHttpWebhookInvoker webhookInvoker;
  private final PSPipelineRequestTraceStore traceStore;

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
    this.binaryAdapter = new PSPipelineBinaryAdapter();
    this.preHooks = preHooks != null ? List.copyOf(preHooks) : List.of();
    this.postHooks = postHooks != null ? List.copyOf(postHooks) : List.of();
    this.webhookInvoker = new PSPipelineHttpWebhookInvoker();
    this.traceStore = PSPipelineRequestTraceStore.getInstance();
  }

  /**
   * Package-visible for tests that isolate last-trace storage from the process singleton.
   */
  PSPipelineRuntimeService(
      IPSPipelineIrService irService,
      IPSPipelineSqlAdapter sqlAdapter,
      IPSPipelineHttpAdapter httpAdapter,
      List<IPSPipelinePreExecuteHook> preHooks,
      List<IPSPipelinePostExecuteHook> postHooks,
      PSPipelineRequestTraceStore traceStore) {
    this.irService = Objects.requireNonNull(irService, "irService");
    this.sqlAdapter = Objects.requireNonNull(sqlAdapter, "sqlAdapter");
    this.httpAdapter = httpAdapter != null ? httpAdapter : new PSPipelineHttpAdapter();
    this.binaryAdapter = new PSPipelineBinaryAdapter();
    this.preHooks = preHooks != null ? List.copyOf(preHooks) : List.of();
    this.postHooks = postHooks != null ? List.copyOf(postHooks) : List.of();
    this.webhookInvoker = new PSPipelineHttpWebhookInvoker();
    this.traceStore = traceStore != null ? traceStore : PSPipelineRequestTraceStore.getInstance();
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
  public PipelineBinaryPayload retrieveBinary(String appName, String resourceName)
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
    return binaryAdapter.retrieve(appName, resource);
  }

  @Override
  public PipelineRequestTrace getLastTrace(String appName) {
    return traceStore.get(appName).orElse(null);
  }

  @Override
  public void clearLastTrace(String appName) {
    traceStore.clear(appName);
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
    boolean tracing = document.getApp() != null && document.getApp().isTracingEnabled();
    StageClock clock = tracing ? new StageClock() : null;
    String appName = document.getApp() != null ? document.getApp().getName() : null;
    try {
      PipelineHookContext ctx = new PipelineHookContext(document, resource, req);
      if (clock != null) {
        clock.mark();
      }
      for (IPSPipelinePreExecuteHook hook : preHooks) {
        hook.beforeExecute(ctx);
      }
      if (clock != null) {
        clock.stage("preHooks", "ok", null);
      }
      webhookInvoker.invokePre(ctx);
      if (clock != null) {
        String preStatus =
            ctx.getPreWebhookStatus() != null ? String.valueOf(ctx.getPreWebhookStatus()) : "skip";
        clock.stage("preWebhook", "ok", preStatus);
      }

      PipelineExecuteResult result = new PipelineExecuteResult();
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
        rows = applyHttpFilterGroup(resource, rows, req);
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
      } else if (PipelineResourceIr.KIND_BINARY.equals(resource.getKind())
          || (resource.getBinary() != null && resource.getBinary().isPresent())) {
        PipelineBinaryPayload payload = binaryAdapter.retrieve(appName, resource);
        result.setOperation("binary");
        result.setKind(PipelineResourceIr.KIND_BINARY);
        result.setRowCount(0);
        result.getMeta().put("contentType", payload.getContentType());
        result.getMeta().put("byteLength", payload.getByteLength());
        result.getMeta().put("path", payload.getPath());
        // Fixture bytes themselves are returned on GET …/binary (not invented JSON rows).
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
      if (clock != null) {
        clock.stage("adapter", "ok", result.getOperation());
      }
      applyResultPageHtml(resource, req, result, clock);

      for (IPSPipelinePostExecuteHook hook : postHooks) {
        hook.afterExecute(ctx, result);
      }
      if (clock != null) {
        clock.stage("postHooks", "ok", null);
      }
      webhookInvoker.invokePost(ctx, result);
      if (clock != null) {
        clock.stage("postWebhook", "ok", null);
      }
      if (ctx.getPreWebhookStatus() != null) {
        result.getMeta().put("preWebhookStatus", ctx.getPreWebhookStatus());
      }
      if (ctx.getPreWebhookBody() != null && !ctx.getPreWebhookBody().isBlank()) {
        result.getMeta().put("preWebhookBody", ctx.getPreWebhookBody());
      }
      // Post hooks may append to context trace; publish full ordered trace on the result.
      result.setHookTrace(new ArrayList<>(ctx.getHookTrace()));

      if (clock != null) {
        publishTrace(appName, resource.getName(), result.getOperation(), req, clock, null);
      }
      return result;
    } catch (PSPipelineIrException | RuntimeException e) {
      if (clock != null) {
        clock.stage("error", "error", e.getClass().getSimpleName());
        publishTrace(appName, resource.getName(), null, req, clock, e.getMessage());
      }
      throw e;
    }
  }

  private void publishTrace(
      String appName,
      String resourceName,
      String operation,
      PipelineExecuteRequest req,
      StageClock clock,
      String error) {
    if (appName == null || appName.isBlank()) {
      return;
    }
    PipelineRequestTrace trace = new PipelineRequestTrace();
    trace.setAppName(appName);
    trace.setResourceName(resourceName);
    trace.setCapturedAt(Instant.now().toString());
    trace.setTracingEnabled(true);
    trace.setOperation(operation);
    trace.setTotalDurationMs(clock.totalMs());
    trace.setStages(clock.stages);
    trace.setRequestParams(PSPipelineRequestTraceSanitizer.sanitizeParams(req.getParams()));
    if (error != null && !error.isBlank()) {
      trace.setError(error);
    }
    traceStore.put(appName, trace);
  }

  /** NanoTime stage stopwatch used only when application tracing is enabled. */
  private static final class StageClock {
    private final List<PipelineRequestTraceStage> stages = new ArrayList<>();
    private final long started = System.nanoTime();
    private long mark = started;

    void mark() {
      mark = System.nanoTime();
    }

    void stage(String name, String status, String detail) {
      long now = System.nanoTime();
      long durationMs = Math.max(0L, (now - mark) / 1_000_000L);
      stages.add(new PipelineRequestTraceStage(name, durationMs, status, detail));
      mark = now;
    }

    long totalMs() {
      return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }
  }

  private static void applyResultPageHtml(
      PipelineResourceIr resource,
      PipelineExecuteRequest req,
      PipelineExecuteResult result,
      StageClock clock)
      throws PSPipelineIrException {
    PipelineResultPageIr page = resource.getResultPage();
    if (PSPipelineXslMerge.wantsJson(req)) {
      result.getMeta().put("contentType", "application/json");
      result.getMeta().put("resultPageApplied", false);
      result.getMeta().put("presentation", PipelineResultPageIr.PRESENTATION_NONE);
      return;
    }
    if (PSPipelineXslMerge.wantsXml(req)) {
      result.setXml(PSPipelineXslMerge.rowsToXml(result.getRows()));
      result.getMeta().put("contentType", "text/xml");
      result.getMeta().put("resultPageApplied", false);
      result.getMeta().put("presentation", PipelineResultPageIr.PRESENTATION_NONE);
      if (clock != null) {
        clock.stage("resultPage", "skipped", "xml");
      }
      return;
    }
    if (!PSPipelineXslMerge.shouldApplyXsl(page, req)) {
      if (PSPipelineXslMerge.wantsHtml(req)) {
        result.setXml(PSPipelineXslMerge.rowsToXml(result.getRows()));
        result.getMeta().put("contentType", "application/xml");
        result.getMeta().put("resultPageApplied", false);
        result.getMeta().put("presentation", PipelineResultPageIr.PRESENTATION_NONE);
      }
      return;
    }
    String html =
        PSPipelineXslMerge.merge(result.getAppName(), page, result.getRows(), null);
    result.setHtml(html);
    result.getMeta().put("contentType", page.resolvedMimeType());
    result.getMeta().put("resultPageApplied", true);
    result.getMeta().put("requestExtension", page.resolvedRequestExtension());
    result.getMeta().put("presentation", PipelineResultPageIr.PRESENTATION_HTML);
    if (clock != null) {
      clock.stage("resultPage", "ok", page.resolvedMimeType());
    }
  }

  private static List<Map<String, Object>> applyHttpFilterGroup(
      PipelineResourceIr resource, List<Map<String, Object>> rows, PipelineExecuteRequest req)
      throws PSPipelineIrException {
    SelectorStageIr selector =
        resource.getStages() != null ? resource.getStages().getSelector() : null;
    FilterGroupIr group = selector != null ? selector.getFilterGroup() : null;
    if (!PSPipelineFilterGroup.isPresent(group)) {
      return rows;
    }
    return PSPipelineFilterGroup.filterRows(
        group, rows, req.getParams() != null ? req.getParams() : Map.of());
  }
}
