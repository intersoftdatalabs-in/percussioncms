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

package com.percussion.itemmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Result of creating a new copy or promotable version of an item. */
@XmlRootElement(name = "ItemCopyResult")
@JsonRootName("ItemCopyResult")
public class PSItemCopyResult {

  private String itemId;
  private String folderPath;
  private boolean promotable;

  public PSItemCopyResult() {}

  public PSItemCopyResult(String itemId, String folderPath, boolean promotable) {
    this.itemId = itemId;
    this.folderPath = folderPath;
    this.promotable = promotable;
  }

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getFolderPath() {
    return folderPath;
  }

  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  public boolean isPromotable() {
    return promotable;
  }

  public void setPromotable(boolean promotable) {
    this.promotable = promotable;
  }
}
