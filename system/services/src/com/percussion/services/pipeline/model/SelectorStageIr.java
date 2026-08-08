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
import java.util.Objects;

/** Query selector stage (classic {@code PSDataSelector}). */
public class SelectorStageIr {

  public static final String METHOD_WHERE = "whereClause";
  public static final String METHOD_NATIVE = "nativeStatement";
  public static final String METHOD_UNKNOWN = "unknown";

  private boolean present;
  private boolean unique;
  private String method = METHOD_UNKNOWN;
  /** Legacy count field; kept in sync when {@link #whereClauses} is set. */
  private int whereClauseCount;
  private int sortedColumnCount;
  private String nativeStatement;
  private List<WhereClauseIr> whereClauses = new ArrayList<>();

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method != null ? method : METHOD_UNKNOWN;
  }

  /**
   * Number of WHERE predicates. Prefer {@link #getWhereClauses()} for executable IR; this count
   * remains for older JSON that only stored the inventory size.
   */
  public int getWhereClauseCount() {
    if (whereClauses != null && !whereClauses.isEmpty()) {
      return whereClauses.size();
    }
    return whereClauseCount;
  }

  public void setWhereClauseCount(int whereClauseCount) {
    this.whereClauseCount = whereClauseCount;
  }

  public int getSortedColumnCount() {
    return sortedColumnCount;
  }

  public void setSortedColumnCount(int sortedColumnCount) {
    this.sortedColumnCount = sortedColumnCount;
  }

  public String getNativeStatement() {
    return nativeStatement;
  }

  public void setNativeStatement(String nativeStatement) {
    this.nativeStatement = nativeStatement;
  }

  public List<WhereClauseIr> getWhereClauses() {
    return whereClauses;
  }

  public void setWhereClauses(List<WhereClauseIr> whereClauses) {
    this.whereClauses = whereClauses != null ? whereClauses : new ArrayList<>();
    this.whereClauseCount = this.whereClauses.size();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SelectorStageIr that)) {
      return false;
    }
    return present == that.present
        && unique == that.unique
        && getWhereClauseCount() == that.getWhereClauseCount()
        && sortedColumnCount == that.sortedColumnCount
        && Objects.equals(method, that.method)
        && Objects.equals(nativeStatement, that.nativeStatement)
        && Objects.equals(whereClauses, that.whereClauses);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        present,
        unique,
        method,
        getWhereClauseCount(),
        sortedColumnCount,
        nativeStatement,
        whereClauses);
  }
}
