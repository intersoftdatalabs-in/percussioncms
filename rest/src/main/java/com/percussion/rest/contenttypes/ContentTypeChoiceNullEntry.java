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

/** Optional null entry on a field choice catalog (CD-07). */
@XmlRootElement(name = "ContentTypeChoiceNullEntry")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Null entry added to a content type field choice catalog")
public class ContentTypeChoiceNullEntry {

  @Schema(description = "Stored value; empty string is allowed")
  private String value;

  @Schema(description = "Display label; defaults to value when omitted on PUT")
  private String label;

  @Schema(description = "always | onlyIfNull. PUT default onlyIfNull")
  private String includeWhen;

  @Schema(description = "first | last | sorted. PUT default first")
  private String sortOrder;

  public ContentTypeChoiceNullEntry() {}

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

  public String getIncludeWhen() {
    return includeWhen;
  }

  public void setIncludeWhen(String includeWhen) {
    this.includeWhen = includeWhen;
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
  }
}
