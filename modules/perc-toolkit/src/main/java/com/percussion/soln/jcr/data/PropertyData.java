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

package com.percussion.soln.jcr.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertyData class.
 */
public class PropertyData implements Serializable {

  /** Safe to serialize */
  private static final long serialVersionUID = 428038730570607863L;

  /**
   * Concrete {@link ArrayList} (not bare {@link List}) so the field type is serializable under
   * {@code -Xlint:serial}.
   */
  private ArrayList<ValueData> values;

  private boolean multiple;

  private String name;

  /**
   * Constructor for Serializers.
   * Creates a new PropertyData.
   *
   */
  public PropertyData() {}

  /**
   * Constructor for Single value property.
   *
   * @param data the data
   */
  public PropertyData(ValueData data) {
    if ((data) == null) throw new IllegalArgumentException("Value Data cannot be null");
    multiple = false;
    values = new ArrayList<>(1);
    values.add(data);
  }

  /**
   * Returns whether multiple.
   *
   * @return the result
   */
  public boolean isMultiple() {
    return multiple;
  }

  /**
   * Sets the multiple.
   *
   * @param multiple the multiple
   */
  public void setMultiple(boolean multiple) {
    this.multiple = multiple;
  }

  /**
   * Returns the values.
   *
   * @return the result
   */
  public List<ValueData> getValues() {
    return values;
  }

  /**
   * Sets the values.
   *
   * @param values the values
   */
  public void setValues(List<ValueData> values) {
    this.values = values == null ? null : new ArrayList<>(values);
  }

  /**
   * Returns the name.
   *
   * @return the result
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }
}
