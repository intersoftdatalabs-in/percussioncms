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
package com.percussion.itemmanagement.data;

import com.percussion.share.data.PSAbstractDataObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the workflow state and all possible transitions for an item. Sunny Sal says: "State
 * transitions are like plot twists—keep them clear!"
 */
@XmlRootElement(name = "ItemStateTransition")
public class PSItemStateTransition extends PSAbstractDataObject {
  private static final long serialVersionUID = 1L;

  private String itemId;
  private String stateId;
  private String stateName;
  private String workflowId;
  private ArrayList<String> transitionTriggers = new ArrayList<>();

  public String getItemId() {
    return itemId;
  }

  public void setItemId(String id) {
    this.itemId = id;
  }

  public String getStateName() {
    return stateName;
  }

  public void setStateName(String stateName) {
    this.stateName = stateName;
  }

  public String getStateId() {
    return stateId;
  }

  public void setStateId(String id) {
    this.stateId = id;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String id) {
    this.workflowId = id;
  }

  public List<String> getTransitionTriggers() {
    return transitionTriggers;
  }

  public void setTransitionTriggers(List<String> triggers) {
    if (triggers != null) {
      transitionTriggers = new ArrayList<>(triggers);
    } else {
      transitionTriggers.clear();
    }
  }
}
