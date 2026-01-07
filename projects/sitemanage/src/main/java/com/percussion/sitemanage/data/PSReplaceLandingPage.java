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

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Contains information for replacing the landing page of a (site) section. */
@XmlRootElement(name = "ReplaceLandingPage")
public class PSReplaceLandingPage extends PSAbstractDataObject {

  private static final long serialVersionUID = 1L;

  private String sectionId;
  private String newLandingPageId;
  private String newLandingPageName;
  private String newLandingPageFromState;
  private String newLandingPageToState;
  private String oldLandingPageName;
  private String oldLandingPageFromState;
  private String oldLandingPageToState;

  public String getSectionId() {
    return sectionId;
  }

  public void setSectionId(String id) {
    sectionId = id;
  }

  public String getNewLandingPageId() {
    return newLandingPageId;
  }

  public void setNewLandingPageId(String id) {
    newLandingPageId = id;
  }

  public Optional<String> getNewLandingPageName() {
    return Optional.ofNullable(newLandingPageName);
  }

  public void setNewLandingPageName(String name) {
    newLandingPageName = name;
  }

  public Optional<String> getNewLandingPageFromState() {
    return Optional.ofNullable(newLandingPageFromState);
  }

  public void setNewLandingPageFromState(String state) {
    newLandingPageFromState = state;
  }

  public Optional<String> getNewLandingPageToState() {
    return Optional.ofNullable(newLandingPageToState);
  }

  public void setNewLandingPageToState(String state) {
    newLandingPageToState = state;
  }

  public Optional<String> getOldLandingPageName() {
    return Optional.ofNullable(oldLandingPageName);
  }

  public void setOldLandingPageName(String name) {
    oldLandingPageName = name;
  }

  public Optional<String> getOldLandingPageFromState() {
    return Optional.ofNullable(oldLandingPageFromState);
  }

  public void setOldLandingPageFromState(String state) {
    oldLandingPageFromState = state;
  }

  public Optional<String> getOldLandingPageToState() {
    return Optional.ofNullable(oldLandingPageToState);
  }

  public void setOldLandingPageToState(String state) {
    oldLandingPageToState = state;
  }
}
