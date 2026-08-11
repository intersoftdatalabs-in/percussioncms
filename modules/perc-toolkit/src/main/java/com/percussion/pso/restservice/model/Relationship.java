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

/**
 * Base REST model for a relationship between content items.
 */
public class Relationship extends ItemRef {
  /**
   * Creates a new Relationship.
   */
  public Relationship() {
    // default
  }


  /** Field relId. */
  private int relId;

  /**
   * Method getRelId.
   *
   * @return int
   */
  @XmlAttribute
  public int getRelId() {
    return relId;
  }

  /**
   * Method setRelId.
   *
   * @param relId int
   */
  public void setRelId(int relId) {
    this.relId = relId;
  }
}
