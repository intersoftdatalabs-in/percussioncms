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

import jakarta.xml.bind.annotation.XmlRootElement;

/** One field in an item-revision compare result. */
@XmlRootElement(name = "ItemRevisionFieldDiff")
public class PSItemRevisionFieldDiff {

  private String name;
  private String leftValue;
  private String rightValue;
  private boolean changed;

  public PSItemRevisionFieldDiff() {}

  public PSItemRevisionFieldDiff(
      String name, String leftValue, String rightValue, boolean changed) {
    this.name = name;
    this.leftValue = leftValue;
    this.rightValue = rightValue;
    this.changed = changed;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLeftValue() {
    return leftValue;
  }

  public void setLeftValue(String leftValue) {
    this.leftValue = leftValue;
  }

  public String getRightValue() {
    return rightValue;
  }

  public void setRightValue(String rightValue) {
    this.rightValue = rightValue;
  }

  public boolean isChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }
}
