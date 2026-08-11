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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.data;

import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Optional;

import java.util.ArrayList;
@XmlRootElement(name = "SiteArchitecture")
public class PSSiteArchitecture extends PSAbstractPersistantObject {

  private static final long serialVersionUID = 8249374630117416709L;

  private String name;
  private ArrayList<PSSiteSection> sections;

  @Override
  public String getId() {
    return getName();
  }

  @Override
  public void setId(String id) {
    setName(id);
  }

  /**
   * @return The name of the site, never blank.
   */
  public String getName() {
    return name;
  }

  /**
   * @param name of the site must not be blank.
   */
  public void setName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = name;
  }

  /**
   * @return the sub sections of site, may be empty.
   */
  public Optional<List<PSSiteSection>> getSections() {
    return Optional.ofNullable(sections);
  }

  /**
   * @param sections sub sections of site, may be null or empty.
   */
  @SuppressWarnings("unchecked")
  public void setSections(List<PSSiteSection> sections) {
    if (sections == null) {
      this.sections = null;
    } else if (sections instanceof ArrayList) {
      this.sections = (ArrayList<PSSiteSection>) sections;
    } else {
      this.sections = new ArrayList<>(sections);
    }
  }
}
