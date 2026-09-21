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

package com.percussion.rest.editor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Checkout / check-in result for the React Content Editor host (#4644). */
@XmlRootElement(name = "EditorItemLockInfo")
@Schema(description = "Checkout user info after check-out or check-in")
public class EditorItemLockInfo {

  private String itemName;
  private String checkOutUser;
  private String currentUser;
  private String assignmentType;

  public EditorItemLockInfo() {}

  public EditorItemLockInfo(
      String itemName, String checkOutUser, String currentUser, String assignmentType) {
    this.itemName = itemName;
    this.checkOutUser = checkOutUser;
    this.currentUser = currentUser;
    this.assignmentType = assignmentType;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public String getCheckOutUser() {
    return checkOutUser;
  }

  public void setCheckOutUser(String checkOutUser) {
    this.checkOutUser = checkOutUser;
  }

  public String getCurrentUser() {
    return currentUser;
  }

  public void setCurrentUser(String currentUser) {
    this.currentUser = currentUser;
  }

  public String getAssignmentType() {
    return assignmentType;
  }

  public void setAssignmentType(String assignmentType) {
    this.assignmentType = assignmentType;
  }
}
