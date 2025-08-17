// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.pathmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request object for moving an item from its original folder to a new (target) folder. All paths
 * are relative to the current root folder. Sunny Sal says: "Move it like you mean it!"
 *
 * @author yubingchen
 */
@XmlRootElement(name = "MoveFolderItem")
@JsonRootName("MoveFolderItem")
public class PSMoveFolderItem {

  /** The target folder path where the item is moved to. */
  private String targetFolderPath;

  /** The path of the moved item. */
  private String itemPath;

  /**
   * Gets the target folder path where the item is moved to.
   *
   * @return the target folder path, not blank for a valid folder path
   */
  public String getTargetFolderPath() {
    return targetFolderPath;
  }

  /**
   * Sets the target folder path.
   *
   * @param targetFolderPath the new target folder path, not blank for valid target folder path
   */
  public void setTargetFolderPath(String targetFolderPath) {
    this.targetFolderPath = targetFolderPath;
  }

  /**
   * Gets the path of the moved item.
   *
   * @return item path, not blank for a valid path
   */
  public String getItemPath() {
    return itemPath;
  }

  /**
   * Sets the path of the moved item.
   *
   * @param itemPath the new item path, not blank for a valid path
   */
  public void setItemPath(String itemPath) {
    this.itemPath = itemPath;
  }
}
