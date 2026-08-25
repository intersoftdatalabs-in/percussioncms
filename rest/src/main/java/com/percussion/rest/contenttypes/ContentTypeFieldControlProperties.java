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
import com.percussion.rest.DesignGap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Control property values and choice catalog for one content type field (CD-07).
 *
 * <p>Jackson root wrap is {@code ContentTypeFieldControlProperties}. GET always returns {@code
 * properties} (may be empty). PUT is a full replace of {@code properties} (empty clears). {@code
 * choices} omitted on PUT leaves the catalog unchanged; present replaces (type {@code none}
 * clears).
 */
@XmlRootElement(name = "ContentTypeFieldControlProperties")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Field control property values and choice catalog (CD-07)")
public class ContentTypeFieldControlProperties {

  @Schema(description = "Field submit name (path {fieldName})")
  private String fieldName;

  @Schema(description = "Display control name; not changed by PUT")
  private String control;

  @Schema(
      required = true,
      description =
          "Control parameter name/value pairs. GET: always present (may be []). PUT: required;"
              + " empty clears.")
  private List<ContentTypeControlProperty> properties;

  @Schema(
      description =
          "Choice catalog. GET: omitted when none. PUT: omit/null leave unchanged; present"
              + " replaces; type none/empty clears.")
  private ContentTypeChoiceCatalog choices;

  @Schema(description = "Structured capability notes vs full Workbench. GET always present.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public ContentTypeFieldControlProperties() {}

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getControl() {
    return control;
  }

  public void setControl(String control) {
    this.control = control;
  }

  public List<ContentTypeControlProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<ContentTypeControlProperty> properties) {
    this.properties = properties;
  }

  public ContentTypeChoiceCatalog getChoices() {
    return choices;
  }

  public void setChoices(ContentTypeChoiceCatalog choices) {
    this.choices = choices;
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
