// REFACTORED: CP-JAVA11
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
package com.percussion.share.data;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.text.Collator;

/**
 * The base class for all named data objects. All named data objects should extend this class or
 * some derivative.
 */
public abstract class PSAbstractNamedObject extends PSAbstractDataObject
    implements Comparable<PSAbstractNamedObject> {

  private static final long serialVersionUID = 1L;

  protected String name;

  /**
   * Gets the name that uniquely identifies the object.
   *
   * @return the name; should not be null or empty unless the object is not finished being processed
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name that uniquely identifies the object.
   *
   * @param name the name to set
   */
  public final void setName(String name) {
    this.name = name;
  }

  /**
   * Determines if the specified name is valid for this object. By default, a valid name is not
   * blank.
   *
   * @param name the name to check
   * @return true if the name is valid, false otherwise
   */
  protected boolean isValidName(String name) {
    return !isBlank(name);
  }

  @Override
  public int compareTo(PSAbstractNamedObject o) {
    return Collator.getInstance().compare(this.getName(), o.getName());
  }
}
