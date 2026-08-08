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
 * JSON response document for pipeline execute.
 *
 * <p>Query results use {@link #rows}; update paths set {@link #affectedRows}.
 */
public class PipelineExecuteResult {

  private String appName;
  private String resourceName;
  private String kind;
  private String operation;
  private int rowCount;
  private int affectedRows;
  private List<Map<String, Object>> rows = new ArrayList<>();
  private List<String> hookTrace = new ArrayList<>();
  private Map<String, Object> meta = new LinkedHashMap<>();

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

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public int getRowCount() {
    return rowCount;
  }

  public void setRowCount(int rowCount) {
    this.rowCount = rowCount;
  }

  public int getAffectedRows() {
    return affectedRows;
  }

  public void setAffectedRows(int affectedRows) {
    this.affectedRows = affectedRows;
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public void setRows(List<Map<String, Object>> rows) {
    this.rows = rows != null ? rows : new ArrayList<>();
    this.rowCount = this.rows.size();
  }

  public List<String> getHookTrace() {
    return hookTrace;
  }

  public void setHookTrace(List<String> hookTrace) {
    this.hookTrace = hookTrace != null ? hookTrace : new ArrayList<>();
  }

  public Map<String, Object> getMeta() {
    return meta;
  }

  public void setMeta(Map<String, Object> meta) {
    this.meta = meta != null ? new LinkedHashMap<>(meta) : new LinkedHashMap<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineExecuteResult that)) {
      return false;
    }
    return rowCount == that.rowCount
        && affectedRows == that.affectedRows
        && Objects.equals(appName, that.appName)
        && Objects.equals(resourceName, that.resourceName)
        && Objects.equals(kind, that.kind)
        && Objects.equals(operation, that.operation)
        && Objects.equals(rows, that.rows)
        && Objects.equals(hookTrace, that.hookTrace)
        && Objects.equals(meta, that.meta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        appName, resourceName, kind, operation, rowCount, affectedRows, rows, hookTrace, meta);
  }
}
