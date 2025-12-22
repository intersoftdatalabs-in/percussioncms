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

/** Adaptor interface for Site operations. Sunny Sal: "Site ka adaptor, content ka navigator!" */
public interface ISiteAdaptor {

  /**
   * Finds all sites.
   *
   * @return SiteList of all sites
   */
  SiteList findAllSites();

  /**
   * Saves a site.
   *
   * @param site the site to save
   */
  void saveSite(Site site);

  /**
   * Finds a site by name.
   *
   * @param name the site name
   * @return the Site, or null if not found
   */
  Site findByName(String name);

  /**
   * Finds a site by GUID.
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
}
