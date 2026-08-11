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
package com.percussion.pso.preview;

import com.percussion.pso.utils.PSOMutableUrl;
import com.percussion.server.PSRequestParsingException;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.HashMap;
import java.util.Map;

/**
 * Location bean.
 *
 * @author DavidBenua
 */
public class SiteFolderLocation {
  private String folderPath;
  private int folderid;
  private IPSSite site;

  /**
   * Default Constructor
   * Creates a new SiteFolderLocation.
   *
   */
  public SiteFolderLocation() {}

  /**
   * Gets the parameter map for this location. These parameters are unique to that location.
   *
   * @return the map.
   */
  public Map<String, Object> getParameterMap() {
    Map<String, Object> pmap = new HashMap<String, Object>();
    pmap.put(IPSHtmlParameters.SYS_SITEID, String.valueOf(getSiteid()));
    pmap.put(IPSHtmlParameters.SYS_FOLDERID, String.valueOf(getFolderid()));
    return pmap;
  }

  /**
   * Fixes a URL by applying the parameters in the map
   *
   * @param baseUrl the base url
   * @return the url with the parameters added.
   * @throws PSRequestParsingException if an error occurs
   */
  public String fixUrl(String baseUrl) throws PSRequestParsingException {
    PSOMutableUrl url = new PSOMutableUrl(baseUrl);
    url.setParamList(this.getParameterMap());
    return url.toString();
  }

  /**
   * Returns the siteName.
   * @return the siteName
   */
  public String getSiteName() {
    return site.getName();
  }

  /**
   * Returns the folderPath.
   * @return the folderPath
   */
  public String getFolderPath() {
    return folderPath;
  }

  /**
   * Sets the folderPath.
   * @param folderPath the folderPath to set
   */
  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

  /**
   * Returns the siteid.
   * @return the siteid
   */
  public long getSiteid() {
    return site.getSiteId();
  }

  /**
   * Returns the folderid.
   * @return the folderid
   */
  public int getFolderid() {
    return folderid;
  }

  /**
   * Sets the folderid.
   * @param folderid the folderid to set
   */
  public void setFolderid(int folderid) {
    this.folderid = folderid;
  }

  /**
   * Returns the site.
   * @return the site
   */
  public IPSSite getSite() {
    return site;
  }

  /**
   * Sets the site.
   * @param site the site to set
   */
  public void setSite(IPSSite site) {
    this.site = site;
  }
}
