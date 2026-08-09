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

/**
 * Backend data tank stage (classic {@code PSBackEndDataTank}).
 *
 * <p>{@link #joins} holds the join-graph edges used by the multi-table SQL planner. {@link
 * #joinCount} remains for inventory / legacy IR and should match {@code joins.size()} when edges
 * were imported.
 */
public class BackendTankStageIr {

  private boolean present;
  private List<BackendTableRefIr> tables = new ArrayList<>();
  private List<BackendJoinIr> joins = new ArrayList<>();
  private int joinCount;

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
        && Objects.equals(joins, that.joins);
  }

  @Override
  public int hashCode() {
    return Objects.hash(present, tables, joins, joinCount);
  }
}
