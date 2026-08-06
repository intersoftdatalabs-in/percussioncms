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
package com.percussion.webservices.ui.data;

/** DTO for a display format column used by converters. */
public class PSDisplayFormatColumnsColumn {
  private String name;
  private String label;
  private String description;
  private boolean category;
  private boolean defaultSortColumn;
  private PSDisplayFormatColumnsColumnRenderType renderType;
  private PSDisplayFormatColumnsColumnSortOrder sortOrder;
  private long sequence;
  private int width;

  public PSDisplayFormatColumnsColumn() {}

  public PSDisplayFormatColumnsColumn(
      String name,
      String label,
      String description,
      boolean category,
      boolean defaultSortColumn,
      PSDisplayFormatColumnsColumnRenderType renderType,
      PSDisplayFormatColumnsColumnSortOrder sortOrder,
      long sequence,
      int width) {
    this.name = name;
    this.label = label;
    this.description = description;
    this.category = category;
    this.defaultSortColumn = defaultSortColumn;
    this.renderType = renderType;
    this.sortOrder = sortOrder;
    this.sequence = sequence;
    this.width = width;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isCategory() {
    return category;
  }

  public void setCategory(boolean category) {
    this.category = category;
  }

  public boolean isDefaultSortColumn() {
    return defaultSortColumn;
  }

  public void setDefaultSortColumn(boolean defaultSortColumn) {
    this.defaultSortColumn = defaultSortColumn;
  }

  public PSDisplayFormatColumnsColumnRenderType getRenderType() {
    return renderType;
  }

  public void setRenderType(PSDisplayFormatColumnsColumnRenderType renderType) {
    this.renderType = renderType;
  }

  public PSDisplayFormatColumnsColumnSortOrder getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(PSDisplayFormatColumnsColumnSortOrder sortOrder) {
    this.sortOrder = sortOrder;
  }

  public long getSequence() {
    return sequence;
  }

  public void setSequence(long sequence) {
    this.sequence = sequence;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }
}
