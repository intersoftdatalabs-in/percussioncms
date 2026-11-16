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

package com.percussion.rest.sharedfields;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Where-used list for one shared field: every content type that includes the field. {@code
 * usedBy} is never {@code null} — empty when no content type includes the field.
 */
@XmlRootElement(name = "SharedFieldUsage")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Content types using one shared field")
public class SharedFieldUsage {

  private String groupName;
  private String fieldName;
  private List<SharedFieldUsageEntry> usedBy = new ArrayList<>();

  public SharedFieldUsage() {}

  @Schema(description = "Shared field group holding the field")
  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  @Schema(description = "Shared field submit name")
  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  @Schema(description = "Content types including the field, name-sorted, never null")
  public List<SharedFieldUsageEntry> getUsedBy() {
    return usedBy;
  }

  public void setUsedBy(List<SharedFieldUsageEntry> usedBy) {
    this.usedBy = usedBy != null ? usedBy : new ArrayList<>();
  }
}
