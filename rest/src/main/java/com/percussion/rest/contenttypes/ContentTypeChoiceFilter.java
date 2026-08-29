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
import java.util.List;

/**
 * Choice filter that replaces the unfiltered catalog with a lookup driven by dependent fields
 * (CD-07).
 */
@XmlRootElement(name = "ContentTypeChoiceFilter")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Choice filter for a content type field control")
public class ContentTypeChoiceFilter {

  @Schema(required = true, description = "Dependent fields; at least one is required on PUT")
  private List<ContentTypeChoiceFilterField> dependentFields;

  @Schema(required = true, description = "Lookup href used to generate the filtered catalog")
  private String lookupHref;

  @Schema(description = "Optional lookup request name")
  private String lookupName;

  public ContentTypeChoiceFilter() {}

  public List<ContentTypeChoiceFilterField> getDependentFields() {
    return dependentFields;
  }

  public void setDependentFields(List<ContentTypeChoiceFilterField> dependentFields) {
    this.dependentFields = dependentFields;
  }

  public String getLookupHref() {
    return lookupHref;
  }

  public void setLookupHref(String lookupHref) {
    this.lookupHref = lookupHref;
  }

  public String getLookupName() {
    return lookupName;
  }

  public void setLookupName(String lookupName) {
    this.lookupName = lookupName;
  }
}
