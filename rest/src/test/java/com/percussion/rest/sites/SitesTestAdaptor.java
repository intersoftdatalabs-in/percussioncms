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

package com.percussion.rest.sites;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link ISiteAdaptor} so rest {@code MainTest} / shared contexts can inject a
 * bean. Production wiring is {@code SitesAdaptor} in sitemanage. PUT echoes the envelope
 * (including {@code sql-database}, {@code http-json}, {@code object-storage}, {@code rss-atom},
 * {@code icalendar}, {@code sitemap-xml}, and {@code robots-txt} {@code sourceKind}); JDBC credentials, HTTP secrets,
 * object-storage cloud credentials, rss-atom live feed credentials, CalDAV credentials, and
 * sitemap crawl credentials are never logged. REST Build
 * for {@code http-json}, {@code object-storage}, {@code rss-atom}, {@code icalendar}, {@code
 * sitemap-xml}, and {@code robots-txt} uses the same adaptor contract as git/CSV/SQL (local JSON fixture / loopback,
 * local object-key bucket, local RSS/Atom fixture, local RFC 5545 {@code calendar.ics}, local
 * {@code sitemap.xml}, or local {@code robots.txt}; {@code virtual.remoteUrl} stays 400). REST Publish copies last-build HTML
 * to {@code IPSSite.root} for git/CSV/SQL/{@code http-json}/object-storage/{@code
 * rss-atom}/{@code icalendar}/{@code sitemap-xml} (local sitemap.xml fixture; leftover
 * {@code virtual.remoteUrl}, credentials, and cloud URL {@code rootPath} are 400; no live crawl).
 * Preview status is last-output based ({@code available=false} when no build), including {@code
 * http-json}, {@code object-storage}, {@code rss-atom}, {@code icalendar}, {@code
 * sitemap-xml}, and {@code robots-txt} last-build output ({@code rss-atom} is a local RSS 2.0 / Atom fixture or
 * loopback feed; {@code icalendar} is a local RFC 5545 fixture only — no CalDAV;
 * {@code sitemap-xml} and {@code robots-txt} are last-build local HTML only — no live crawl).
 */
@Component
@Lazy
public class SitesTestAdaptor implements ISiteAdaptor {

  @Override
  public SiteList findAllSites() {
    return new SiteList();
  }

  @Override
  public void saveSite(Site site) {
    // No-op for test adaptor
  }

  @Override
  public Site findByName(String name) {
    return null;
  }

  @Override
  public Site findByGuid(String guid) {
    return null;
  }

  @Override
  public void deleteSite(Site site) {
    // No-op for test adaptor
  }

  @Override
  public Site createSite() {
    return null;
  }

  @Override
  public VirtualSiteProperties getVirtualSiteProperties(String nameOrId) {
    return new VirtualSiteProperties();
  }

  @Override
  public VirtualSiteProperties updateVirtualSiteProperties(
      String nameOrId, VirtualSiteProperties props) {
    return props != null ? props : new VirtualSiteProperties();
  }

  @Override
  public VirtualSiteBuildResult buildVirtualSite(
      String nameOrId, VirtualSiteBuildRequest request) {
    VirtualSiteBuildResult result = new VirtualSiteBuildResult();
    result.setSiteName(nameOrId);
    result.setSiteKey(nameOrId);
    result.setPagesWritten(0);
    result.setLinkProblemCount(0);
    result.setHasLinkProblems(false);
    if (request != null && request.getOutputRoot() != null) {
      result.setOutputPath(request.getOutputRoot());
    }
    return result;
  }

  @Override
  public VirtualSitePreviewStatus getVirtualSitePreviewStatus(String nameOrId) {
    VirtualSitePreviewStatus status = new VirtualSitePreviewStatus();
    status.setAvailable(false);
    status.setMessage("No assembled Virtual Site to preview.");
    return status;
  }

  @Override
  public VirtualSitePreviewFile previewVirtualSiteFile(String nameOrId, String relativePath) {
    throw new jakarta.ws.rs.WebApplicationException(
        "No assembled Virtual Site to preview.", jakarta.ws.rs.core.Response.Status.NOT_FOUND);
  }

  @Override
  public VirtualSitePublishResult publishVirtualSite(String nameOrId) {
    VirtualSitePublishResult result = new VirtualSitePublishResult();
    result.setSiteName(nameOrId);
    result.setSiteKey(nameOrId);
    result.setPagesWritten(0);
    result.setFilesCopied(0);
    result.setLinkProblemCount(0);
    result.setHasLinkProblems(false);
    return result;
  }
}
