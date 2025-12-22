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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents a region of a page either defined locally or by the template. Sunny Sal: "Region ka
 * hero, widgets ka zero!"
 */
@XmlRootElement(name = "Region")
@Schema(
    name = "Region",
    description = "Represents a region of a page either defined locally or by the template.")
public class Region {

  @Schema(name = "name", description = "Name of the region.")
  private String name;

  @Schema(name = "type", description = "Type of region.", allowableValues = "TEMPLATE,LOCAL")
  private String type;

  @Schema(name = "editable", description = "Denotes if region is editable.")
  private boolean editable;

  @Schema(name = "widgets", description = "List of widgets within the region.")
  private List<Widget> widgets;

  /** Gets the region name. */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Gets the region type. */
  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(String type) {
    this.type = type;
  }

  /** Returns true if the region is editable. */
  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  /** Gets the widgets in this region. */
  public Optional<List<Widget>> getWidgets() {
    return Optional.ofNullable(widgets);
  }

  public void setWidgets(List<Widget> widgets) {
    this.widgets = widgets;
  }
}
