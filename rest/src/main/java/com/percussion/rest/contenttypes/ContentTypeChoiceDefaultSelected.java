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

/** Default-selected choice on a field catalog (CD-07). */
@XmlRootElement(name = "ContentTypeChoiceDefaultSelected")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Default-selected entry for a content type field choice catalog")
public class ContentTypeChoiceDefaultSelected {

  @Schema(
      required = true,
      description = "nullEntry | sequence | text. nullEntry uses the catalog null-entry.")
  private String type;

  @Schema(description = "Zero-based sequence when type is sequence")
  private Integer sequence;

  @Schema(description = "Matching entry value when type is text")
  private String text;

  public ContentTypeChoiceDefaultSelected() {}

  public ContentTypeChoiceDefaultSelected(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Integer getSequence() {
    return sequence;
  }

  public void setSequence(Integer sequence) {
    this.sequence = sequence;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
