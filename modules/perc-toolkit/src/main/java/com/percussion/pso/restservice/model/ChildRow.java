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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

/**
 * REST model for a single row within a child field group.
 */
public class ChildRow {

  /**
   * Creates a new ChildRow.
   */
  public ChildRow() {
    // default
  }
  /** Field fields. */
  private List<Field> fields = null;

  /**
   * Method setFields.
   *
   * @param fields List of Field
   */
  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  /**
   * Method getFields.
   *
   * @return List of Field
   */
  @XmlElement(name = "Field")
  @XmlElementWrapper(name = "Fields")
  public List<Field> getFields() {
    return fields;
  }
}
