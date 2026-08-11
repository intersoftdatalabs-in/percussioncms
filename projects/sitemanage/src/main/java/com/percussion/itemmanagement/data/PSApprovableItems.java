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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
/** Wrapper class to hold the list of {@link PSApprovableItem}s and their processing status. */
@XmlRootElement(name = "ApprovableItems")
public class PSApprovableItems extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  /** List of approvable items associated to the gadget. */
  private ArrayList<PSApprovableItem> approvableItems;

  /** List of items that have been processed. */
  private ArrayList<PSApprovableItem> processedItems;

  /** Map of errors encountered during processing. */
  private HashMap<String, String> errors = new HashMap<>();

  public PSApprovableItems() {
    // Default constructor for JAX-RS
  }

  public List<PSApprovableItem> getApprovableItems() {
    return approvableItems;
  }

  @SuppressWarnings("unchecked")
  public void setApprovableItems(List<PSApprovableItem> approvableItems) {
    if (approvableItems == null) {
      this.approvableItems = null;
    } else if (approvableItems instanceof ArrayList) {
      this.approvableItems = (ArrayList<PSApprovableItem>) approvableItems;
    } else {
      this.approvableItems = new ArrayList<>(approvableItems);
    }
  }

  public Map<String, String> getErrors() {
    return errors;
  }

  @SuppressWarnings("unchecked")
  public void setErrors(Map<String, String> errors) {
    if (errors == null) {
      this.errors = null;
    } else if (errors instanceof HashMap) {
      this.errors = (HashMap<String, String>) errors;
    } else {
      this.errors = new HashMap<>(errors);
    }
  }

  public List<PSApprovableItem> getProcessedItems() {
    return processedItems;
  }

  @SuppressWarnings("unchecked")
  public void setProcessedItems(List<PSApprovableItem> processedItems) {
    if (processedItems == null) {
      this.processedItems = null;
    } else if (processedItems instanceof ArrayList) {
      this.processedItems = (ArrayList<PSApprovableItem>) processedItems;
    } else {
      this.processedItems = new ArrayList<>(processedItems);
    }
  }
}
