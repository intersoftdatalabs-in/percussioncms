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

package com.percussion.rest.folders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Explorer item properties (name / display title) (#4701). */
@XmlRootElement(name = "ItemProperties")
@JsonRootName("ItemProperties")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Item name and display title")
public class ItemProperties {

  private String itemPath;
  private String name;
  private String displayTitle;

  public ItemProperties() {
    // Default constructor
  }

  public ItemProperties(String itemPath, String name, String displayTitle) {
    this.itemPath = itemPath;
    this.name = name;
    this.displayTitle = displayTitle;
  }

  public String getItemPath() {
    return itemPath;
  }

  public void setItemPath(String itemPath) {
    this.itemPath = itemPath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayTitle() {
    return displayTitle;
  }

  public void setDisplayTitle(String displayTitle) {
    this.displayTitle = displayTitle;
  }
}
