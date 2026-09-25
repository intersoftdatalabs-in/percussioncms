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

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Workflows the server will accept when reassigning one item (#4861). */
@XmlRootElement(name = "ItemWorkflowChoices")
public class PSItemWorkflowChoices extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String itemId;
  private String currentWorkflowId;
  private ArrayList<PSItemWorkflowChoice> choices = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  public String getCurrentWorkflowId() {
    return currentWorkflowId;
  }

  public void setCurrentWorkflowId(String currentWorkflowId) {
    this.currentWorkflowId = currentWorkflowId;
  }

  public List<PSItemWorkflowChoice> getChoices() {
    return choices;
  }

  public void setChoices(List<PSItemWorkflowChoice> choices) {
    this.choices = choices == null ? new ArrayList<>() : new ArrayList<>(choices);
  }
}
