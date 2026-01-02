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
<<<<<<< HEAD
import com.percussion.system.utils.IPSHtmlParameters;
=======
import com.percussion.util.IPSHtmlParameters;
>>>>>>> development-8.1.x
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

  /** Default Constructor */
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
   * @param baseUrl
   * @return the url with the parameters added.
   * @throws PSRequestParsingException
   */
  public String fixUrl(String baseUrl) throws PSRequestParsingException {
    PSOMutableUrl url = new PSOMutableUrl(baseUrl);
    url.setParamList(this.getParameterMap());
    return url.toString();
  }

<<<<<<< HEAD
  /**
   * @return the siteName
   */
=======
  /** @return the siteName */
>>>>>>> development-8.1.x
  public String getSiteName() {
    return site.getName();
  }

<<<<<<< HEAD
  /**
   * @return the folderPath
   */
=======
  /** @return the folderPath */
>>>>>>> development-8.1.x
  public String getFolderPath() {
    return folderPath;
  }

<<<<<<< HEAD
  /**
   * @param folderPath the folderPath to set
   */
=======
  /** @param folderPath the folderPath to set */
>>>>>>> development-8.1.x
  public void setFolderPath(String folderPath) {
    this.folderPath = folderPath;
  }

<<<<<<< HEAD
  /**
   * @return the siteid
   */
=======
  /** @return the siteid */
>>>>>>> development-8.1.x
  public long getSiteid() {
    return site.getSiteId();
  }

<<<<<<< HEAD
  /**
   * @return the folderid
   */
=======
  /** @return the folderid */
>>>>>>> development-8.1.x
  public int getFolderid() {
    return folderid;
  }

<<<<<<< HEAD
  /**
   * @param folderid the folderid to set
   */
=======
  /** @param folderid the folderid to set */
>>>>>>> development-8.1.x
  public void setFolderid(int folderid) {
    this.folderid = folderid;
  }

<<<<<<< HEAD
  /**
   * @return the site
   */
=======
  /** @return the site */
>>>>>>> development-8.1.x
  public IPSSite getSite() {
    return site;
  }

<<<<<<< HEAD
  /**
   * @param site the site to set
   */
=======
  /** @param site the site to set */
>>>>>>> development-8.1.x
  public void setSite(IPSSite site) {
    this.site = site;
  }
}
