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

// REFACTORED: CP-JAVA11

package com.percussion.rest.pages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.percussion.rest.assets.Asset;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents a Widget. Sunny Sal: "Widget ka hero, content ka zero!" */
@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement(name = "Widget")
@Schema(name = "Widget", description = "Represents a Widget.")
public class Widget implements Cloneable {

  public static final String SCOPE_LOCAL = "local";
  public static final String SCOPE_SHARED = "shared";

  @Schema(name = "id", description = "Id of the widget.")
  private String id;

  @Schema(name = "name", description = "Name of the widget.")
  private String name;

  @Schema(name = "type", description = "Type of widget.")
  private String type;

  @Schema(name = "scope", description = "Scope of the widget.", allowableValues = "local,shared")
  private String scope;

  @Schema(name = "editable", description = "Denotes if widget is editable.")
  private Boolean editable;

  @Schema(name = "asset", description = "Asset within the widget.")
  private Asset asset;

  public Widget() {
    // Required for JSON
  }

  /** Gets the widget id. */
  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(String id) {
    this.id = id;
  }

  /** Gets the widget name. */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Gets the widget type. */
  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  /** Gets the widget scope. */
  public Optional<String> getScope() {
    return Optional.ofNullable(scope);
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  /** Gets whether the widget is editable. */
  public Optional<Boolean> getEditable() {
    return Optional.ofNullable(editable);
  }

  public void setEditable(Boolean editable) {
    this.editable = editable;
  }

  /** Gets the asset within the widget. */
  public Optional<Asset> getAsset() {
    return Optional.ofNullable(asset);
  }

  public void setAsset(Asset asset) {
    this.asset = asset;
  }

  @Override
  protected Widget clone() throws CloneNotSupportedException {
    return (Widget) super.clone();
  }
}
