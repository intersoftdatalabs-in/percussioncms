/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.delivery.utils.lookup;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * Single entry in a {@link PSLookup}. Each entry pairs a {@code label} with a {@code value},
 * modeled on the legacy {@code sys_Lookup/PSXEntry} XML structure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PSXEntry {

  /** The display label associated with this entry. */
  @XmlElement(name = "PSXDisplayText")
  private String label;

  /** The internal value associated with this entry. */
  @XmlElement(name = "Value")
  private String value;

  /** Default constructor. */
  public PSXEntry() {}

  /**
   * Constructs a new entry with the supplied value and label.
   *
   * @param value the value, may be <code>null</code>.
   * @param label the display label, may be <code>null</code>.
   */
  public PSXEntry(String value, String label) {
    this.label = label;
    this.value = value;
  }

  /**
   * Returns the internal value associated with this entry.
   *
   * @return the value, may be <code>null</code>.
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the internal value associated with this entry.
   *
   * @param value the value to set, may be <code>null</code>.
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Returns the display label associated with this entry.
   *
   * @return the label, may be <code>null</code>.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the display label associated with this entry.
   *
   * @param label the label to set, may be <code>null</code>.
   */
  public void setLabel(String label) {
    this.label = label;
  }
}
