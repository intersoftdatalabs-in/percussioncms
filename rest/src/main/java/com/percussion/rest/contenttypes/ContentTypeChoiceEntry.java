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

/** Local choice catalog entry (label/value) for a field control (CD-07). */
@XmlRootElement(name = "ContentTypeChoiceEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Local choice entry for a content type field control")
public class ContentTypeChoiceEntry {

  @Schema(required = true, description = "Stored value")
  private String value;

  @Schema(description = "Display label; defaults to value when omitted on PUT")
  private String label;

  public ContentTypeChoiceEntry() {}

  public ContentTypeChoiceEntry(String value, String label) {
    this.value = value;
    this.label = label;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
