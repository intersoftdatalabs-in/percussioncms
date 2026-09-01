/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.views;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ViewDef")
@Schema(description = "CX view definition (UI-07)")
public class ViewDef {

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
  private boolean standardView;
  private boolean customView;
  private boolean view;
  private boolean userCustomizable;
  private boolean caseSensitive;
  /** Null on PUT means leave existing criteria unchanged; empty list clears. */
  private List<ViewFieldSummary> fields;
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

  public boolean isStandardView() {
    return standardView;
  }

  public void setStandardView(boolean standardView) {
    this.standardView = standardView;
  }

  public boolean isCustomView() {
    return customView;
  }

  public void setCustomView(boolean customView) {
    this.customView = customView;
  }

  public boolean isView() {
    return view;
  }

  public void setView(boolean view) {
    this.view = view;
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

  public List<ViewFieldSummary> getFields() {
    return fields;
  }

  public void setFields(List<ViewFieldSummary> fields) {
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
