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
 * { "rows": [ { "TYPE": "workflow", "NAME": "wf1" } ] }
 * </pre>
 */
public class PipelineExecuteRequest {

  private Map<String, Object> params = new LinkedHashMap<>();
  private List<Map<String, Object>> rows = new ArrayList<>();

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineExecuteRequest that)) {
      return false;
    }
    return Objects.equals(params, that.params) && Objects.equals(rows, that.rows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params, rows);
  }
}
