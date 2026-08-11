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
 * REST model container for the relationships of a content item.
 */
public class Relationships {
  /**
   * Creates a new Relationships.
   */
  public Relationships() {
    // default
  }

  /** Field slots. */
  List<Slot> slots = null;

  /** Field copies. */
  List<Copy> copies = null;

  /** Field translations. */
  private List<Translation> translations;

  /**
   * Method getTranslations.
   *
   * @return List of Translation
   */
  @XmlElementWrapper(name = "Translations")
  @XmlElement(name = "Translation")
  public List<Translation> getTranslations() {
    return translations;
  }

  /**
   * Method setTranslations.
   *
   * @param translations List of Translation
   */
  public void setTranslations(List<Translation> translations) {
    this.translations = translations;
  }

  /**
   * Method getSlots.
   *
   * @return List of Slot
   */
  @XmlElementWrapper(name = "Slots")
  @XmlElement(name = "Slot")
  public List<Slot> getSlots() {
    return slots;
  }

  /**
   * Method setSlots.
   *
   * @param slots List of Slot
   */
  public void setSlots(List<Slot> slots) {
    this.slots = slots;
  }

  /**
   * Method getCopies.
   *
   * @return List of Copy
   */
  @XmlElementWrapper(name = "Copies")
  @XmlElement(name = "Copy")
  public List<Copy> getCopies() {
    return copies;
  }

  /**
   * Method setCopies.
   *
   * @param copies List of Copy
   */
  public void setCopies(List<Copy> copies) {
    this.copies = copies;
  }
}
