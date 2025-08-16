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

package com.percussion.itemmanagement.data;

import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.data.PSItemProperties;
import java.util.HashSet;
import java.util.Set;

/**
 * Java representation of the Site Impact of an Asset. Contains the pages, templates, and sites that
 * own the asset.
 */
public class PSAssetSiteImpact {

  private Set<PSItemProperties> ownerPages;
  private Set<PSTemplateSummary> ownerTemplates;
  private Set<IPSSite> ownerSites;

  public PSAssetSiteImpact() {
    ownerPages = new HashSet<>();
    ownerTemplates = new HashSet<>();
    ownerSites = new HashSet<>();
  }

  /**
   * Gets the templates that own the asset.
   *
   * @return the ownerTemplates
   */
  public Set<PSTemplateSummary> getOwnerTemplates() {
    return ownerTemplates;
  }

  /**
   * Sets the templates that own the asset.
   *
   * @param ownerTemplates the ownerTemplates to set
   */
  public void setOwnerTemplates(Set<PSTemplateSummary> ownerTemplates) {
    this.ownerTemplates = ownerTemplates;
  }

  /**
   * Gets the pages that own the asset.
   *
   * @return the ownerPages
   */
  public Set<PSItemProperties> getOwnerPages() {
    return ownerPages;
  }

  /**
   * Sets the pages that own the asset.
   *
   * @param ownerPages the ownerPages to set
   */
  public void setOwnerPages(Set<PSItemProperties> ownerPages) {
    this.ownerPages = ownerPages;
  }

  /**
   * Gets the sites that own the asset.
   *
   * @return the ownerSites
   */
  public Set<IPSSite> getOwnerSites() {
    return ownerSites;
  }

  /**
   * Sets the sites that own the asset.
   *
   * @param ownerSites the ownerSites to set
   */
  public void setOwnerSites(Set<IPSSite> ownerSites) {
    this.ownerSites = ownerSites;
  }
}
