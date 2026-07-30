/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.rest.locales;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Read-only CMS locale catalog row (CD-18). */
@XmlRootElement(name = "LocaleSummary")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CMS locale summary")
public class LocaleSummary {

  private Integer id;
  private String languageString;
  private String label;
  private String description;
  private String status;

  /**
   * True when RXLOCALE.ISBASE marks a language-only / base locale (e.g. {@code ar}, {@code es}).
   */
  private Boolean baseLocale;

  /**
   * True when an RXLOCALEFORMAT row exists for this language string (exact key). Format resolution
   * may still fall back without a row.
   */
  private Boolean hasFormatProfile;

  public LocaleSummary() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getLanguageString() {
    return languageString;
  }

  public void setLanguageString(String languageString) {
    this.languageString = languageString;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Boolean getBaseLocale() {
    return baseLocale;
  }

  public void setBaseLocale(Boolean baseLocale) {
    this.baseLocale = baseLocale;
  }

  public Boolean getHasFormatProfile() {
    return hasFormatProfile;
  }

  public void setHasFormatProfile(Boolean hasFormatProfile) {
    this.hasFormatProfile = hasFormatProfile;
  }
}
