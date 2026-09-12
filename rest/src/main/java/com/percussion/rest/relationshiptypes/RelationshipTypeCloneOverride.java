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

package com.percussion.rest.relationshiptypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Cloning field override on a relationship type ({@code PSCloneOverrideField}).
 *
 * <p>The replacement value is a UDF {@code PSExtensionCall}. Effect/clone-override
 * <em>conditions</em> are not writable on this surface (see remaining {@code designGaps}).
 */
@XmlRootElement(name = "RelationshipTypeCloneOverride")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Cloning field override (content field + UDF extension call)")
public class RelationshipTypeCloneOverride {

  @Schema(description = "Content editor field name to override on clone (e.g. sys_title)")
  private String fieldName;

  @Schema(
      description =
          "Fully qualified UDF extension ref (e.g."
              + " Java/global/percussion/cms/sys_cloneOverrideField)")
  private String extensionRef;

  @Schema(description = "Literal parameters bound to the extension call, in order")
  private List<String> extensionParams = new ArrayList<>();

  public RelationshipTypeCloneOverride() {}

  public RelationshipTypeCloneOverride(String fieldName, String extensionRef) {
    this.fieldName = fieldName;
    this.extensionRef = extensionRef;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getExtensionRef() {
    return extensionRef;
  }

  public void setExtensionRef(String extensionRef) {
    this.extensionRef = extensionRef;
  }

  public List<String> getExtensionParams() {
    return extensionParams;
  }

  public void setExtensionParams(List<String> extensionParams) {
    this.extensionParams = extensionParams;
  }
}
