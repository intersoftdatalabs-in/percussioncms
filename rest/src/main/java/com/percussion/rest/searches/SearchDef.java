/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.searches;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "SearchDef")
@Schema(description = "CX search definition (UI-06)")
public class SearchDef {

  private Guid guid;
  private int id;
  private String name;
  private String label;
  private String description;
  private String type;
  private String displayFormatId;
  private String url;
  private int parentCategory;
  private int maximumResultSize;
  private boolean userSearch;
  private boolean customSearch;
  private boolean standardSearch;
  private boolean userCustomizable;
  private boolean caseSensitive;
  private List<SearchFieldSummary> fields = new ArrayList<>();
  private List<String> designGaps = new ArrayList<>();

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDisplayFormatId() {
    return displayFormatId;
  }

  public void setDisplayFormatId(String displayFormatId) {
    this.displayFormatId = displayFormatId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public int getParentCategory() {
    return parentCategory;
  }

  public void setParentCategory(int parentCategory) {
    this.parentCategory = parentCategory;
  }

  public int getMaximumResultSize() {
    return maximumResultSize;
  }

  public void setMaximumResultSize(int maximumResultSize) {
    this.maximumResultSize = maximumResultSize;
  }

  public boolean isUserSearch() {
    return userSearch;
  }

  public void setUserSearch(boolean userSearch) {
    this.userSearch = userSearch;
  }

  public boolean isCustomSearch() {
    return customSearch;
  }

  public void setCustomSearch(boolean customSearch) {
    this.customSearch = customSearch;
  }

  public boolean isStandardSearch() {
    return standardSearch;
  }

  public void setStandardSearch(boolean standardSearch) {
    this.standardSearch = standardSearch;
  }

  public boolean isUserCustomizable() {
    return userCustomizable;
  }

  public void setUserCustomizable(boolean userCustomizable) {
    this.userCustomizable = userCustomizable;
  }

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public List<SearchFieldSummary> getFields() {
    return fields;
  }

  public void setFields(List<SearchFieldSummary> fields) {
    this.fields = fields;
  }

  /**
   * Catalog-level capability notes. Present on detail; omitted on list rows when null/empty
   * (REST-GAPS-02 payload dedup).
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Schema(
      description =
          "Honest design gaps for this surface. Present on detail GET; typically omitted on"
              + " list rows to avoid repeating the same catalog-level array")
  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
