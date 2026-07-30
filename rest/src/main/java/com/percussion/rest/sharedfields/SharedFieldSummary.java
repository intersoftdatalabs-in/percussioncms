/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.sharedfields;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Read-only field row inside a shared field group. */
@XmlRootElement(name = "SharedField")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Shared field summary")
public class SharedFieldSummary {

  private String name;
  private String dataType;
  private Boolean searchable;
  private Boolean required;
  private Boolean readOnly;
  private String occurrence;

  public SharedFieldSummary() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  public Boolean getSearchable() {
    return searchable;
  }

  public void setSearchable(Boolean searchable) {
    this.searchable = searchable;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public Boolean getReadOnly() {
    return readOnly;
  }

  public void setReadOnly(Boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getOccurrence() {
    return occurrence;
  }

  public void setOccurrence(String occurrence) {
    this.occurrence = occurrence;
  }
}
