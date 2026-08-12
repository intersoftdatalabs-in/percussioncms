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

package com.percussion.rest.contentexplorer.folders;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Named folder property (RX {@code PSFolderProperty} wire shape). */
@XmlRootElement(name = "RxFolderProperty")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Folder property name/value pair")
public class RxFolderProperty {

  @Schema(description = "Property name")
  private String name;

  @Schema(description = "Property value")
  private String value;

  @Schema(description = "Optional description")
  private String description;

  public RxFolderProperty() {}

  public RxFolderProperty(String name, String value, String description) {
    this.name = name;
    this.value = value;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
