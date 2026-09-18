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
import java.util.ArrayList;
import java.util.List;

/** Field-level compare of two revisions of one item. */
@XmlRootElement(name = "ItemRevisionCompare")
public class PSItemRevisionCompareResult {

  private String itemId;
  private int rev1;
  private int rev2;
  private List<PSItemRevisionFieldDiff> fields = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public int getRev1() {
    return rev1;
  }

  public void setRev1(int rev1) {
    this.rev1 = rev1;
  }

  public int getRev2() {
    return rev2;
  }

  public void setRev2(int rev2) {
    this.rev2 = rev2;
  }

  public List<PSItemRevisionFieldDiff> getFields() {
    return fields;
  }

  public void setFields(List<PSItemRevisionFieldDiff> fields) {
    this.fields = fields == null ? new ArrayList<>() : fields;
  }
}
