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

package com.percussion.rest.contenttypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import java.util.List;

/**
 * Choice catalog for a field control (CD-07).
 *
 * <p>{@code type} is {@code global}, {@code local}, {@code lookup}, {@code internalLookup}, or
 * {@code tableinfo}. PUT {@code type=none} (or empty) clears the catalog. When this object is
 * present on PUT, {@code filter}, {@code nullEntry}, and {@code defaultSelected} are written as
 * part of the catalog replace (omit/null clears those extras).
 */
@XmlRootElement(name = "ContentTypeChoiceCatalog")
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlSeeAlso({
  ContentTypeChoiceEntry.class,
  ContentTypeChoiceTable.class,
  ContentTypeChoiceFilter.class,
  ContentTypeChoiceFilterField.class,
  ContentTypeChoiceNullEntry.class,
  ContentTypeChoiceDefaultSelected.class
})
@Schema(description = "Choice catalog for a content type field control")
public class ContentTypeChoiceCatalog {

  @Schema(
      description =
          "global | local | lookup | internalLookup | tableinfo. PUT none/empty clears choices.")
  private String type;

  @Schema(description = "ascending | descending | user")
  private String sortOrder;

  @Schema(description = "Keyword / global lookup table id when type is global")
  private Integer globalId;

  @Schema(description = "Local entries when type is local")
  private List<ContentTypeChoiceEntry> entries;

  @Schema(description = "Lookup href when type is lookup or internalLookup")
  private String lookupHref;

  @Schema(description = "Optional lookup request name")
  private String lookupName;

  @Schema(description = "Table info when type is tableinfo")
  private ContentTypeChoiceTable table;

  @Schema(
      description =
          "Choice filter. GET: omitted when none. PUT: omit/null clears; present replaces.")
  private ContentTypeChoiceFilter filter;

  @Schema(
      description =
          "Null entry added to the catalog. GET: omitted when none. PUT: omit/null clears.")
  private ContentTypeChoiceNullEntry nullEntry;

  @Schema(
      description =
          "Default-selected entries. GET: omitted when none. PUT: omit/null/empty clears.")
  private List<ContentTypeChoiceDefaultSelected> defaultSelected;

  public ContentTypeChoiceCatalog() {}

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }

  public Integer getGlobalId() {
    return globalId;
  }

  public void setGlobalId(Integer globalId) {
    this.globalId = globalId;
  }

  public List<ContentTypeChoiceEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<ContentTypeChoiceEntry> entries) {
    this.entries = entries;
  }

  public String getLookupHref() {
    return lookupHref;
  }

  public void setLookupHref(String lookupHref) {
    this.lookupHref = lookupHref;
  }

  public String getLookupName() {
    return lookupName;
  }

  public void setLookupName(String lookupName) {
    this.lookupName = lookupName;
  }

  public ContentTypeChoiceTable getTable() {
    return table;
  }

  public void setTable(ContentTypeChoiceTable table) {
    this.table = table;
  }

  public ContentTypeChoiceFilter getFilter() {
    return filter;
  }

  public void setFilter(ContentTypeChoiceFilter filter) {
    this.filter = filter;
  }

  public ContentTypeChoiceNullEntry getNullEntry() {
    return nullEntry;
  }

  public void setNullEntry(ContentTypeChoiceNullEntry nullEntry) {
    this.nullEntry = nullEntry;
  }

  public List<ContentTypeChoiceDefaultSelected> getDefaultSelected() {
    return defaultSelected;
  }

  public void setDefaultSelected(List<ContentTypeChoiceDefaultSelected> defaultSelected) {
    this.defaultSelected = defaultSelected;
  }
}
