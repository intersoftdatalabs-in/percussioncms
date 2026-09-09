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

package com.percussion.services.pipeline.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Last request-trace snapshot for a native pipeline application (Slice D).
 *
 * <p>Fail-closed: params are sanitized (no passwords, tokens, or Authorization values). Result
 * rows are never copied onto this document.
 */
public class PipelineRequestTrace {

  private String appName;
  private String resourceName;
  private String capturedAt;
  private boolean tracingEnabled;
  private String operation;
  private long totalDurationMs;
  private List<PipelineRequestTraceStage> stages = new ArrayList<>();
  private Map<String, Object> requestParams = new LinkedHashMap<>();
  private String error;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getResourceName() {
    return resourceName;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public String getCapturedAt() {
    return capturedAt;
  }

  public void setCapturedAt(String capturedAt) {
    this.capturedAt = capturedAt;
  }

  public boolean isTracingEnabled() {
    return tracingEnabled;
  }

  public void setTracingEnabled(boolean tracingEnabled) {
    this.tracingEnabled = tracingEnabled;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public long getTotalDurationMs() {
    return totalDurationMs;
  }

  public void setTotalDurationMs(long totalDurationMs) {
    this.totalDurationMs = totalDurationMs;
  }

  public List<PipelineRequestTraceStage> getStages() {
    return stages;
  }

  public void setStages(List<PipelineRequestTraceStage> stages) {
    this.stages = stages != null ? stages : new ArrayList<>();
  }

  public Map<String, Object> getRequestParams() {
    return requestParams;
  }

  public void setRequestParams(Map<String, Object> requestParams) {
    this.requestParams =
        requestParams != null ? new LinkedHashMap<>(requestParams) : new LinkedHashMap<>();
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineRequestTrace that)) {
      return false;
    }
    return tracingEnabled == that.tracingEnabled
        && totalDurationMs == that.totalDurationMs
        && Objects.equals(appName, that.appName)
        && Objects.equals(resourceName, that.resourceName)
        && Objects.equals(capturedAt, that.capturedAt)
        && Objects.equals(operation, that.operation)
        && Objects.equals(stages, that.stages)
        && Objects.equals(requestParams, that.requestParams)
        && Objects.equals(error, that.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        appName,
        resourceName,
        capturedAt,
        tracingEnabled,
        operation,
        totalDurationMs,
        stages,
        requestParams,
        error);
  }
}
