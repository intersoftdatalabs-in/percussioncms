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

/**
 * Adaptor interface for Site operations. Sunny Sal: "Site ka adaptor, content ka navigator!"
 *
 * <p>Virtual Site properties ({@code virtual.*}) are exposed via {@link
 * #getVirtualSiteProperties(String)} / {@link #updateVirtualSiteProperties(String,
 * VirtualSiteProperties)} and are also populated on {@link Site#getVirtual()} for detail loads.
 * Traditional sites expose {@link Site#getManagedNavigation()} ({@code navigation.managed});
 * Virtual Sites omit that flag.
 */
public interface ISiteAdaptor {

  /**
   * Finds all sites.
   *
   * @return SiteList of all sites
   */
  SiteList findAllSites();

  /**
   * Saves a site (general fields). Virtual Site bag uses {@link
   * #updateVirtualSiteProperties(String, VirtualSiteProperties)}.
   *
   * @param site the site to save
   */
  void saveSite(Site site);

  /**
   * Finds a site by name (detail includes {@link Site#getVirtual()} when configured).
   *
   * @param name the site name
   * @return the Site, or null if not found
   */
  Site findByName(String name);

  /**
   * Finds a site by GUID string (detail includes {@link Site#getVirtual()} when configured).
   *
   * @param guid the site GUID
   * @return the Site, or null if not found
   */
  Site findByGuid(String guid);

  /**
   * Deletes the given site.
   *
   * @param site the site to delete
   */
  void deleteSite(Site site);

  /**
   * Creates a new Site.
   *
   * @return the new Site
   */
  Site createSite();

  /**
   * Loads Virtual Site properties for a site identified by name or GUID string.
   *
   * @param nameOrId site name or GUID string, not blank
   * @return properties (never null; fields may be empty for traditional Sites)
   * @throws jakarta.ws.rs.WebApplicationException 404 when site not found
   */
  VirtualSiteProperties getVirtualSiteProperties(String nameOrId);

  /**
   * Creates or updates Virtual Site properties for a site identified by name or GUID string.
   * Validation aligns with {@code PSVirtualSiteHelper} (source-kind allow-list {@code
   * git-filesystem} / {@code csv-filesystem}, required root path when virtual and remote is blank,
   * optional remoteUrl/branch for git-filesystem only, safe path / config file name). GET after PUT
   * round-trips the stored {@code sourceKind}.
   *
   * @param nameOrId site name or GUID string, not blank
   * @param props properties to apply; not null
   * @return persisted properties after save
   * @throws jakarta.ws.rs.WebApplicationException 400 on validation failure, 404 when site not found
   */
  VirtualSiteProperties updateVirtualSiteProperties(String nameOrId, VirtualSiteProperties props);

  /**
   * Builds a Virtual Site from configured {@code virtual.*} properties ({@code git-filesystem} or
   * {@code csv-filesystem}).
   *
   * <p>Loads the site, validates via {@code PSVirtualSiteHelper}, optionally clones/fetches {@code
   * virtual.remoteUrl} into a contained work directory (git-filesystem only), runs {@code
   * PSVirtualSiteBuildService.forSourceType} with portable NIO {@code Path} I/O, and returns
   * pages-written plus link-problem summary. Unknown source kinds return 400. Requires Admin (or
   * equivalent site-manage) authorization.
   *
   * @param nameOrId site name or GUID string, not blank
   * @param request optional body (output root override); may be null
   * @return build summary (never null)
   * @throws jakarta.ws.rs.WebApplicationException 400 for repository / invalid virtual config /
   *     path issues, 403 when not authorized, 404 when site not found
   */
  VirtualSiteBuildResult buildVirtualSite(String nameOrId, VirtualSiteBuildRequest request);

  /**
   * Reports whether the last Virtual Site static build can be previewed (assembled home exists).
   *
   * <p>Last-output based: {@code git-filesystem} and {@code csv-filesystem} sites are both
   * previewable after a successful assemble. Missing output is {@code available=false} with a
   * message (not a 500). Repository and unknown source kinds are 400. Requires Admin.
   *
   * @param nameOrId site name or GUID string, not blank
   * @return status (never null)
   * @throws jakarta.ws.rs.WebApplicationException 400 when the site is not Virtual or sourceKind is
   *     unknown, 403 when not authorized, 404 when site not found
   */
  VirtualSitePreviewStatus getVirtualSitePreviewStatus(String nameOrId);

  /**
   * Streams one file from the last Virtual Site build output (path-traversal safe).
   *
   * <p>Same last-output contract as {@link #getVirtualSitePreviewStatus} for {@code git-filesystem}
   * and {@code csv-filesystem}.
   *
   * @param nameOrId site name or GUID string, not blank
   * @param relativePath path under the output root ({@code 8.2/index.html}); blank means assembled
   *     home
   * @return file bytes and media type (never null)
   * @throws jakarta.ws.rs.WebApplicationException 400 unsafe path / not virtual / unknown
   *     sourceKind / file larger than 20 MB, 403 not Admin, 404 site or file missing
   */
  VirtualSitePreviewFile previewVirtualSiteFile(String nameOrId, String relativePath);

  /**
   * Builds a Virtual Site ({@code git-filesystem} or {@code csv-filesystem}) and copies the static
   * output to the Site filesystem publish root ({@code IPSSite.getRoot()}).
   *
   * <p>Publish-includes-build: operators get a published docs tree at the configured Site
   * publishing location, not only {@code tmp/virtual-sites}. Failures are operator-facing 4xx (not
   * a silent no-op). Requires Admin.
   *
   * @param nameOrId site name or GUID string, not blank
   * @return publish summary (never null)
   * @throws jakarta.ws.rs.WebApplicationException 400 when the Site is not virtual, publish root is
   *     missing/unsafe, or the source tree overlaps the target; 403 when not authorized; 404 when
   *     site not found
   */
  VirtualSitePublishResult publishVirtualSite(String nameOrId);
}
