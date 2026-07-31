/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
