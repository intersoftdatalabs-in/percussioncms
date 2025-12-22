// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.ui.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.ArrayList;
import java.util.List;

/** A simplified version of PSDisplayFormat for CMS list views. */
@JsonRootName("SimpleDisplayFormat")
public class PSSimpleDisplayFormat {
  private int id;
  private String name;
  private String displayName;
  private String description;
  private List<PSDisplayFormatColumn> columns = new ArrayList<>();
  private String sortBy;
  private boolean sortAscending = true;

  /** Gets the display format ID. */
  public int getId() {
    return id;
  }

  /** Sets the display format ID. */
  public void setId(int id) {
    this.id = id;
  }

  /** Gets the internal name. */
  public String getName() {
    return name;
  }

  /** Sets the internal name. */
  public void setName(String name) {
    this.name = name;
  }

  /** Gets the display name. */
  public String getDisplayName() {
    return displayName;
  }

  /** Sets the display name. */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /** Gets the description. */
  public String getDescription() {
    return description;
  }

  /** Sets the description. */
  public void setDescription(String description) {
    this.description = description;
  }

  /** Gets the list of columns. */
  public List<PSDisplayFormatColumn> getColumns() {
    return columns;
  }

  /** Sets the list of columns. */
  public void setColumns(List<PSDisplayFormatColumn> columns) {
    this.columns = columns;
  }

  /** Gets the sort by field. */
  public String getSortby() {
    return sortBy;
  }

  /** Sets the sort by field. */
  public void setSortby(String sortBy) {
    this.sortBy = sortBy;
  }

  /** Returns true if sorting is ascending. */
  public boolean isSortAscending() {
    return sortAscending;
  }

  /** Sets whether sorting is ascending. */
  public void setSortAscending(boolean sortAscending) {
    this.sortAscending = sortAscending;
  }
}
