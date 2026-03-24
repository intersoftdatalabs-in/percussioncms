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
package com.percussion.cms.objectstore;

/**
 * This class is used to define rules for elements to be included in the Element returned in {@link
 * PSItemComponent#toXml(Document,PSAcceptElements)} calls. This may be expanded at a later date to
 * provide more flexibility of including and excluding elements based on object values in the
 * sys_StandardItem.xsd.
 */
// REFACTORED: CP-JAVA11
public class PSAcceptElements {
  /**
   * Creates an instance with the boolean rules specified by the parameters.
   *
   * @param includeFields include fields in toXml call
   * @param includeChildren include children in toXml call
   * @param includeRelated include related items in toXml call
   * @param includeBinary include binary fields in toXml call
   */
  public PSAcceptElements(
      boolean includeFields,
      boolean includeChildren,
      boolean includeRelated,
      boolean includeBinary) {
    this.includeFields = includeFields;
    this.includeChildren = includeChildren;
    this.includeRelated = includeRelated;
    this.includeBinary = includeBinary;
  }

  /**
   * Specifies the inclusion of binary values in toXml calls.
   *
   * @return {@code true} if they are to be included, otherwise {@code false}.
   */
  public boolean includeBinary() {
    return includeBinary;
  }

  /** Indicates if fields should be included in a toXml call, default is {@code true}. */
  private boolean includeFields = true;

  /** Indicates if children should be included in a toXml call, default is {@code true}. */
  private boolean includeChildren = true;

  /** Indicates if related items should be included in a toXml call, default is {@code true}. */
  private boolean includeRelated = true;

  /** Indicates if binary fields should be included in a toXml call, default is {@code true}. */
  private boolean includeBinary = true;
}
