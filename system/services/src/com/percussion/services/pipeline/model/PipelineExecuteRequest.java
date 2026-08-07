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
 * JSON request body subset for pipeline execute: named parameters and optional update rows.
 *
 * <p>Example query body:
 *
 * <pre>
 * { "params": { "TYPE": "workflow" } }
 * </pre>
 *
 * <p>Example insert body:
 *
 * <pre>
 * { "operation": "insert", "rows": [ { "TYPE": "workflow", "NAME": "wf1" } ] }
 * </pre>
 *
 * <p>Example update body (SET non-key columns; WHERE from {@code keyColumns} values on each row):
 *
 * <pre>
 * {
 *   "operation": "update",
 *   "keyColumns": ["TYPE", "NAME"],
 *   "rows": [ { "TYPE": "workflow", "NAME": "wf1", "LOOKUPVALUE": "99" } ]
 * }
 * </pre>
 *
 * <p>Example delete body:
 *
 * <pre>
 * {
 *   "operation": "delete",
 *   "keyColumns": ["TYPE", "NAME"],
 *   "rows": [ { "TYPE": "locale", "NAME": "en-us" } ]
 * }
 * </pre>
 *
 * <p>When an UPDATE resource allows only one of insert/update/delete, {@code operation} may be
 * omitted and is inferred. When more than one is allowed, {@code operation} is required.
 */
public class PipelineExecuteRequest {

  public static final String OP_INSERT = "insert";
  public static final String OP_UPDATE = "update";
  public static final String OP_DELETE = "delete";

  private String operation;
  private Map<String, Object> params = new LinkedHashMap<>();
  private List<Map<String, Object>> rows = new ArrayList<>();
  private List<String> keyColumns = new ArrayList<>();

  public static PipelineExecuteRequest ofParams(Map<String, Object> params) {
    PipelineExecuteRequest req = new PipelineExecuteRequest();
    if (params != null) {
      req.params.putAll(params);
    }
    return req;
  }

  public static PipelineExecuteRequest empty() {
    return new PipelineExecuteRequest();
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Map<String, Object> getParams() {
    return params;
  }

  public void setParams(Map<String, Object> params) {
    this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
  }

  public List<Map<String, Object>> getRows() {
    return rows;
  }

  public void setRows(List<Map<String, Object>> rows) {
    this.rows = rows != null ? new ArrayList<>(rows) : new ArrayList<>();
  }

  /**
   * Mapped column names used as equality WHERE keys for update/delete. Values are taken from each
   * row (or from {@link #params} when used as a single-row body).
   */
  public List<String> getKeyColumns() {
    return keyColumns;
  }

  public void setKeyColumns(List<String> keyColumns) {
    this.keyColumns = keyColumns != null ? new ArrayList<>(keyColumns) : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineExecuteRequest that)) {
      return false;
    }
    return Objects.equals(operation, that.operation)
        && Objects.equals(params, that.params)
        && Objects.equals(rows, that.rows)
        && Objects.equals(keyColumns, that.keyColumns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operation, params, rows, keyColumns);
  }
}
