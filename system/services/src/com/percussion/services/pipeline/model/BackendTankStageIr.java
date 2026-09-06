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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Backend data tank stage (classic {@code PSBackEndDataTank}).
 *
 * <p>{@link #joins} holds the join-graph edges used by the multi-table SQL planner. {@link
 * #joinCount} remains for inventory / legacy IR and should match {@code joins.size()} when edges
 * were imported.
 *
 * <p>Slice C HTTP adapter: {@link #adapterType} {@code HTTP} (or {@code REST}) plus {@link #url}
 * (loopback / local fixture only). Blank adapter type remains SQL.
 */
public class BackendTankStageIr {

  public static final String ADAPTER_SQL = "SQL";
  public static final String ADAPTER_HTTP = "HTTP";
  public static final String ADAPTER_REST = "REST";

  private boolean present;
  private List<BackendTableRefIr> tables = new ArrayList<>();
  private List<BackendJoinIr> joins = new ArrayList<>();
  private int joinCount;
  private String adapterType;
  private String url;
  private String httpMethod;

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  public List<BackendTableRefIr> getTables() {
    return tables;
  }

  public void setTables(List<BackendTableRefIr> tables) {
    this.tables = tables != null ? tables : new ArrayList<>();
  }

  public List<BackendJoinIr> getJoins() {
    return joins;
  }

  public void setJoins(List<BackendJoinIr> joins) {
    this.joins = joins != null ? joins : new ArrayList<>();
  }

  public int getJoinCount() {
    return joinCount;
  }

  public void setJoinCount(int joinCount) {
    this.joinCount = joinCount;
  }

  public String getAdapterType() {
    return adapterType;
  }

  public void setAdapterType(String adapterType) {
    this.adapterType = adapterType;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  /** True when this tank is an HTTP/REST datasource (Slice C). */
  public boolean isHttpAdapter() {
    if (adapterType == null || adapterType.isBlank()) {
      return false;
    }
    String n = adapterType.trim().toUpperCase(Locale.ROOT);
    return ADAPTER_HTTP.equals(n) || ADAPTER_REST.equals(n);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BackendTankStageIr that)) {
      return false;
    }
    return present == that.present
        && joinCount == that.joinCount
        && Objects.equals(tables, that.tables)
        && Objects.equals(joins, that.joins)
        && Objects.equals(adapterType, that.adapterType)
        && Objects.equals(url, that.url)
        && Objects.equals(httpMethod, that.httpMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(present, tables, joins, joinCount, adapterType, url, httpMethod);
  }
}
