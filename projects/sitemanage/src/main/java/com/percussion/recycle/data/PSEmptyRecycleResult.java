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
package com.percussion.recycle.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary returned after emptying the Recycling bin.
 *
 * <p>Counts cover top-level children under the Recycling root that were attempted during the bulk
 * purge. {@link #getUndeletedCount()} is the total number of items that could not be permanently
 * purged (permissions, in-use, or errors with skip-on-failure).
 */
@XmlRootElement(name = "EmptyRecycleResult")
@JsonRootName("EmptyRecycleResult")
public class PSEmptyRecycleResult {

  private int purgedFolderCount;
  private int purgedItemCount;
  private int undeletedCount;
  private boolean alreadyEmpty;
  private List<String> errors = new ArrayList<>();

  public int getPurgedFolderCount() {
    return purgedFolderCount;
  }

  public void setPurgedFolderCount(int purgedFolderCount) {
    this.purgedFolderCount = purgedFolderCount;
  }

  public int getPurgedItemCount() {
    return purgedItemCount;
  }

  public void setPurgedItemCount(int purgedItemCount) {
    this.purgedItemCount = purgedItemCount;
  }

  public int getUndeletedCount() {
    return undeletedCount;
  }

  public void setUndeletedCount(int undeletedCount) {
    this.undeletedCount = undeletedCount;
  }

  public boolean isAlreadyEmpty() {
    return alreadyEmpty;
  }

  public void setAlreadyEmpty(boolean alreadyEmpty) {
    this.alreadyEmpty = alreadyEmpty;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors != null ? errors : new ArrayList<>();
  }

  public void addError(String error) {
    if (error != null && !error.isBlank()) {
      this.errors.add(error);
    }
  }

  public void incrementPurgedFolders() {
    purgedFolderCount++;
  }

  public void incrementPurgedItems() {
    purgedItemCount++;
  }

  public void addUndeleted(int count) {
    if (count > 0) {
      undeletedCount += count;
    }
  }
}
