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

package com.percussion.rest.sites;

import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/** Represents a Site in Percussion CMS. Sunny Sal: "Site ka hero, URL ka zero!" */
@XmlRootElement(name = "Site")
@Schema(name = "Site")
public class Site {

  private String name;
  private String description;
  private String baseUrl;
  private String defaultFileExtention;
  private boolean isCanonical = true;
  private boolean overrideSystemJQuery = false;
  private boolean overrideSystemFoundation = false;
  private boolean overrideSystemJQueryUI = false;
  private String siteAdditionalHeadContent;
  private String siteBeforeBodyCloseContent;
  private String siteAfterBodyOpenContent;

  /** Determines canonical URL's protocol ("http" or "https"). */
  private String siteProtocol = "https";

  /**
   * Determines the site's default document (like "index.html") used when rendering canonical tags.
   */
  private String defaultDocument = "index.html";

  private String canonicalDist = "pages";
  private boolean canonicalReplace = true;
  private boolean pageBasedSite = false;
  private Guid guid;

  /**
   * Virtual Site property bag ({@code virtual.*}). Present when loaded with detail; may be {@code
   * null} on summary list payloads.
   */
  @Schema(
      description =
          "Virtual Site properties (virtual.sourceKind, virtual.rootPath, virtual.configFile,"
              + " virtual.siteKey). Null when not loaded.")
  private VirtualSiteProperties virtual;

  public Site() {
    // Default constructor
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Optional<String> getBaseUrl() {
    return Optional.ofNullable(baseUrl);
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Optional<String> getDefaultFileExtention() {
    return Optional.ofNullable(defaultFileExtention);
  }

  public void setDefaultFileExtention(String defaultFileExtention) {
    this.defaultFileExtention = defaultFileExtention;
  }

  public boolean isCanonical() {
    return isCanonical;
  }

  public void setCanonical(boolean canonical) {
    isCanonical = canonical;
  }

  public boolean isOverrideSystemJQuery() {
    return overrideSystemJQuery;
  }

  public void setOverrideSystemJQuery(boolean overrideSystemJQuery) {
    this.overrideSystemJQuery = overrideSystemJQuery;
  }

  public boolean isOverrideSystemFoundation() {
    return overrideSystemFoundation;
  }

  public void setOverrideSystemFoundation(boolean overrideSystemFoundation) {
    this.overrideSystemFoundation = overrideSystemFoundation;
  }

  public boolean isOverrideSystemJQueryUI() {
    return overrideSystemJQueryUI;
  }

  public void setOverrideSystemJQueryUI(boolean overrideSystemJQueryUI) {
    this.overrideSystemJQueryUI = overrideSystemJQueryUI;
  }

  public Optional<String> getSiteAdditionalHeadContent() {
    return Optional.ofNullable(siteAdditionalHeadContent);
  }

  public void setSiteAdditionalHeadContent(String siteAdditionalHeadContent) {
    this.siteAdditionalHeadContent = siteAdditionalHeadContent;
  }

  public Optional<String> getSiteBeforeBodyCloseContent() {
    return Optional.ofNullable(siteBeforeBodyCloseContent);
  }

  public void setSiteBeforeBodyCloseContent(String siteBeforeBodyCloseContent) {
    this.siteBeforeBodyCloseContent = siteBeforeBodyCloseContent;
  }

  public Optional<String> getSiteAfterBodyOpenContent() {
    return Optional.ofNullable(siteAfterBodyOpenContent);
  }

  public void setSiteAfterBodyOpenContent(String siteAfterBodyOpenContent) {
    this.siteAfterBodyOpenContent = siteAfterBodyOpenContent;
  }

  public Optional<String> getSiteProtocol() {
    return Optional.ofNullable(siteProtocol);
  }

  public void setSiteProtocol(String siteProtocol) {
    this.siteProtocol = siteProtocol;
  }

  public Optional<String> getDefaultDocument() {
    return Optional.ofNullable(defaultDocument);
  }

  public void setDefaultDocument(String defaultDocument) {
    this.defaultDocument = defaultDocument;
  }

  public Optional<String> getCanonicalDist() {
    return Optional.ofNullable(canonicalDist);
  }

  public void setCanonicalDist(String canonicalDist) {
    this.canonicalDist = canonicalDist;
  }

  public boolean isCanonicalReplace() {
    return canonicalReplace;
  }

  public void setCanonicalReplace(boolean canonicalReplace) {
    this.canonicalReplace = canonicalReplace;
  }

  public boolean isPageBasedSite() {
    return pageBasedSite;
  }

  public void setPageBasedSite(boolean pageBasedSite) {
    this.pageBasedSite = pageBasedSite;
  }

  public Optional<Guid> getGuid() {
    return Optional.ofNullable(guid);
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public Optional<VirtualSiteProperties> getVirtual() {
    return Optional.ofNullable(virtual);
  }

  public void setVirtual(VirtualSiteProperties virtual) {
    this.virtual = virtual;
  }
}
