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

import java.util.Objects;

/** Update synchronizer stage (classic {@code PSDataSynchronizer} on update pipes). */
public class UpdaterStageIr {

  private boolean present;
  private boolean allowInsert;
  private boolean allowUpdate;
  private boolean allowDelete;
  private int updateColumnCount;

  public boolean isPresent() {
    return present;
  }

  public void setPresent(boolean present) {
    this.present = present;
  }

  public boolean isAllowInsert() {
    return allowInsert;
  }

  public void setAllowInsert(boolean allowInsert) {
    this.allowInsert = allowInsert;
  }

  public boolean isAllowUpdate() {
    return allowUpdate;
  }

  public void setAllowUpdate(boolean allowUpdate) {
    this.allowUpdate = allowUpdate;
  }

  public boolean isAllowDelete() {
    return allowDelete;
  }

  public void setAllowDelete(boolean allowDelete) {
    this.allowDelete = allowDelete;
  }

  public int getUpdateColumnCount() {
    return updateColumnCount;
  }

  public void setUpdateColumnCount(int updateColumnCount) {
    this.updateColumnCount = updateColumnCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UpdaterStageIr that)) {
      return false;
    }
    return present == that.present
        && allowInsert == that.allowInsert
        && allowUpdate == that.allowUpdate
        && allowDelete == that.allowDelete
        && updateColumnCount == that.updateColumnCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(present, allowInsert, allowUpdate, allowDelete, updateColumnCount);
  }
}
