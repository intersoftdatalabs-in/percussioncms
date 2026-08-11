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
   * Validation aligns with {@code PSVirtualSiteHelper} (source-kind allow-list, required root path
   * when virtual, safe path / config file name).
   *
   * @param nameOrId site name or GUID string, not blank
   * @param props properties to apply; not null
   * @return persisted properties after save
   * @throws jakarta.ws.rs.WebApplicationException 400 on validation failure, 404 when site not found
   */
  VirtualSiteProperties updateVirtualSiteProperties(String nameOrId, VirtualSiteProperties props);
}
