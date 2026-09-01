/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.cecontrols;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Content editor control catalog projection (UI-01 read). */
@XmlRootElement(name = "ControlDef")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content editor control definition (system or user)")
public class ControlDef {

  private String name;
  private String displayName;
  private String description;
  private String dimension;
  private String choiceSet;

  /** system or user */
  private String scope;

  private boolean deprecated;
  private String deprecatedReplacement;
  private List<ControlParameter> parameters = new ArrayList<>();
  private List<String> designGaps = new ArrayList<>();

  /**
   * Optional full XSL stylesheet on write. Omitted on list/detail unless the client supplied it.
   * When absent on POST/PUT the server generates a default user-control stylesheet from metadata.
   */
  private String xslSource;

  public ControlDef() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension(String dimension) {
    this.dimension = dimension;
  }

  public String getChoiceSet() {
    return choiceSet;
  }

  public void setChoiceSet(String choiceSet) {
    this.choiceSet = choiceSet;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  public String getDeprecatedReplacement() {
    return deprecatedReplacement;
  }

  public void setDeprecatedReplacement(String deprecatedReplacement) {
    this.deprecatedReplacement = deprecatedReplacement;
  }

  public List<ControlParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<ControlParameter> parameters) {
    this.parameters = parameters;
  }

  /**
   * Catalog-level capability notes. Present on detail; omitted on list rows when null
   * (REST-GAPS-02 payload dedup; class uses NON_NULL).
   */
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

  @Schema(
      description =
          "Optional full XSL stylesheet for POST/PUT. When omitted the server writes a default"
              + " user-control stylesheet from name/displayName/description/dimension/choiceSet."
              + " Not a Developer SPA source editor.")
  public String getXslSource() {
    return xslSource;
  }

  public void setXslSource(String xslSource) {
    this.xslSource = xslSource;
  }
}
