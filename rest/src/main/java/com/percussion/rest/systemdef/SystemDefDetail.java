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

package com.percussion.rest.systemdef;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Content-editor system definition (global system fields). GET catalog; PUT may patch existing
 * field properties ({@code searchable}, occurrence / required). POST {@code /systemdef/fields} and
 * DELETE {@code /systemdef/fields/{fieldName}} create and delete fields.
 */
@XmlRootElement(name = "SystemDefDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content editor system definition field catalog")
public class SystemDefDetail {

  private Integer fieldCount;
  private Integer cacheTimeoutMinutes;
  private List<SystemDefFieldSummary> fields = new ArrayList<>();
  private List<String> designGaps = new ArrayList<>();

  public SystemDefDetail() {}

  public Integer getFieldCount() {
    return fieldCount;
  }

  public void setFieldCount(Integer fieldCount) {
    this.fieldCount = fieldCount;
  }

  public Integer getCacheTimeoutMinutes() {
    return cacheTimeoutMinutes;
  }

  public void setCacheTimeoutMinutes(Integer cacheTimeoutMinutes) {
    this.cacheTimeoutMinutes = cacheTimeoutMinutes;
  }

  public List<SystemDefFieldSummary> getFields() {
    return fields;
  }

  public void setFields(List<SystemDefFieldSummary> fields) {
    this.fields = fields != null ? fields : new ArrayList<>();
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
