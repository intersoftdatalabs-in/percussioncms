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
package com.percussion.integrations.siteimprove.data;

import java.util.Objects;
import java.util.Optional;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Base object model for the publish settings for Siteimprove usage for the assigned site. */
@XmlRootElement(name = "SiteimproveConfiguration")
public class PSSiteImproveSiteConfigurations {

  private String siteName;
  private Boolean doProduction;
  private Boolean doStaging;
  private Boolean doAssetsScanExclude;
  private Boolean doPreview;
  private Boolean isSiteImproveEnabled;

  /** Empty constructor for JAX-RS to use. */
  public PSSiteImproveSiteConfigurations() {
    // Default constructor
  }

  /**
   * @param siteName The name of site the settings are associated with.
   * @param doProduction True/False to have Siteimprove be used on production sites.
   * @param doStaging Siteimprove usage for staging sites.
   * @param doAssetsScanExclude Siteimprove usage for excluding scanning of assets.
   * @param doPreview Siteimprove usage for preview sites.
   * @param isSiteImproveEnabled Enable/disable Siteimprove for this site.
   */
  public PSSiteImproveSiteConfigurations(
      String siteName,
      Boolean doProduction,
      Boolean doStaging,
      Boolean doAssetsScanExclude,
      Boolean doPreview,
      Boolean isSiteImproveEnabled) {
    this.siteName = siteName;
    this.doProduction = doProduction;
    this.doStaging = doStaging;
    this.doAssetsScanExclude = doAssetsScanExclude;
    this.doPreview = doPreview;
    this.isSiteImproveEnabled = isSiteImproveEnabled;
  }

  public String getSiteName() {
    return siteName;
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName;
  }

  public Optional<Boolean> getDoProduction() {
    return Optional.ofNullable(doProduction);
  }

  public void setDoProduction(Boolean doProduction) {
    this.doProduction = doProduction;
  }

  public Optional<Boolean> getDoStaging() {
    return Optional.ofNullable(doStaging);
  }

  public void setDoStaging(Boolean doStaging) {
    this.doStaging = doStaging;
  }

  public Optional<Boolean> getDoAssetsScanExclude() {
    return Optional.ofNullable(doAssetsScanExclude);
  }

  public void setDoAssetsScanExclude(Boolean doAssetsScanExclude) {
    this.doAssetsScanExclude = doAssetsScanExclude;
  }

  public Optional<Boolean> getDoPreview() {
    return Optional.ofNullable(doPreview);
  }

  public void setDoPreview(Boolean doPreview) {
    this.doPreview = doPreview;
  }

  public Optional<Boolean> getIsSiteImproveEnabled() {
    return Optional.ofNullable(isSiteImproveEnabled);
  }

  public void setIsSiteImproveEnabled(Boolean isSiteImproveEnabled) {
    this.isSiteImproveEnabled = isSiteImproveEnabled;
  }

  @Override
  public String toString() {
    return "PSSiteImproveSiteConfigurations{"
        + "siteName='"
        + siteName
        + '\''
        + ", doProduction="
        + doProduction
        + ", doStaging="
        + doStaging
        + ", doAssetsScanExclude="
        + doAssetsScanExclude
        + ", doPreview="
        + doPreview
        + ", isSiteImproveEnabled="
        + isSiteImproveEnabled
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSSiteImproveSiteConfigurations)) return false;
    var that = (PSSiteImproveSiteConfigurations) o;
    return Objects.equals(siteName, that.siteName)
        && Objects.equals(doProduction, that.doProduction)
        && Objects.equals(doStaging, that.doStaging)
        && Objects.equals(doAssetsScanExclude, that.doAssetsScanExclude)
        && Objects.equals(doPreview, that.doPreview)
        && Objects.equals(isSiteImproveEnabled, that.isSiteImproveEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        siteName, doProduction, doStaging, doAssetsScanExclude, doPreview, isSiteImproveEnabled);
  }
}
