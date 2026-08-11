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
package com.percussion.pso.restservice.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * REST model for an assembly slot and its items.
 */
@XmlRootElement(name = "Slot")
public class Slot implements Comparable<Slot> {

  /**
   * Creates a new Slot.
   */
  public Slot() {
    // default
  }

  /** Field name. */
  String name;

  /** Field type. */
  String type;

  /** Field items. */
  List<SlotItem> items;

  /**
   * Method getItems.
   *
   * @return List of SlotItem
   */
  @XmlElement(name = "Item")
  public List<SlotItem> getItems() {
    return items;
  }

  /**
   * Method setItems.
   *
   * @param items List of SlotItem
   */
  public void setItems(List<SlotItem> items) {
    this.items = items;
  }

  /**
   * Method getName.
   *
   * @return String
   */
  @XmlAttribute
  public String getName() {
    return name;
  }

  /**
   * Method setName.
   *
   * @param name String
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Method getType.
   *
   * @return String
   */
  @XmlAttribute
  public String getType() {
    return type;
  }

  /**
   * Method setType.
   *
   * @param type String
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Method compareTo.
   *
   * @param o Slot
   * @return int
   */
  public int compareTo(Slot o) {
    return this.name.compareTo(o.getName());
  }

  /**
   * Method hashCode.
   *
   * @return int
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  /**
   * Method equals.
   *
   * @param obj Object
   * @return boolean
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Slot other = (Slot) obj;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }
}
