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
package com.percussion.pso.restservice.model.results;

import com.percussion.pso.restservice.model.ItemRef;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * PagedResult class.
 */
@XmlRootElement(name = "Results")
public class PagedResult {
  /**
   * Creates a new PagedResult.
   */
  public PagedResult() {
    // default
  }


  List<ItemRef> itemRefs;
  String next;
  Integer nextId;

  /**
   * Returns the next id.
   *
   * @return the result
   */
  @XmlAttribute
  public Integer getNextId() {
    return nextId;
  }

  /**
   * Sets the next id.
   *
   * @param nextId the next id
   */
  public void setNextId(Integer nextId) {
    this.nextId = nextId;
  }

  /**
   * Returns the next.
   *
   * @return the result
   */
  @XmlElement
  public String getNext() {
    return next;
  }

  /**
   * Sets the next.
   *
   * @param next the next
   */
  public void setNext(String next) {
    this.next = next;
  }

  /**
   * Returns the item refs.
   *
   * @return the result
   */
  @XmlElement(name = "Item")
  @XmlElementWrapper(name = "Items")
  public List<ItemRef> getItemRefs() {
    return itemRefs;
  }

  /**
   * Sets the item refs.
   *
   * @param itemRefs the item refs
   */
  public void setItemRefs(List<ItemRef> itemRefs) {
    this.itemRefs = itemRefs;
  }
}
