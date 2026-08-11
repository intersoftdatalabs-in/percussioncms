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
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Builds a URL for Preview. If the assembly URL is not known, use the default URL.
 *
 * @author DavidBenua
 */
public class PreviewUrlBuilder implements UrlBuilder, Cloneable {

  private static final Logger log = LogManager.getLogger(PreviewUrlBuilder.class);

  private String defaultLocationUrl;

  private String multipleLocationUrl;

        /**
     * Creates a new PreviewUrlBuilder.
     */
    public PreviewUrlBuilder() {}

  /**
   * See referenced member.
   * @see UrlBuilder#buildUrl(IPSAssemblyTemplate, Map, SiteFolderLocation, boolean)
   * @param template the template
   * @param urlParams the url params
   * @param location the location
   * @param useMultiple the use multiple
   * @return the result
   * @throws Exception if an error occurs
   */
  public String buildUrl(
      IPSAssemblyTemplate template,
      Map<String, Object> urlParams,
      SiteFolderLocation location,
      boolean useMultiple)
      throws Exception {
    PSOMutableUrl url;
    String defaultLoc = this.getDefaultLocationUrl();

    String templateid = findTemplateId(template);
    if (useMultiple) {
      url = new PSOMutableUrl(this.getMultipleLocationUrl());
      log.debug("multiple url is : {}", url.toString());
    } else {
      url = new PSOMutableUrl(this.getDefaultLocationUrl());
      log.debug("single url is : {}", url.toString());
    }
    Map<String, Object> newParams = new HashMap<String, Object>(urlParams);
    if (location != null) {
      newParams.putAll(location.getParameterMap());
    }
    newParams.put(IPSHtmlParameters.SYS_ITEMFILTER, "preview");
    newParams.put(IPSHtmlParameters.SYS_CONTEXT, "0");

    newParams.put(IPSHtmlParameters.SYS_TEMPLATE, templateid);

    url.setParamList(newParams);
    log.debug("new url is {}", url.toString());
    return url.toString();
  }

  /**
   * Find the template id for the given template.
   *
   * @param template the template
   * @return the template id.
   */
  protected String findTemplateId(IPSAssemblyTemplate template) {
    String templateid = String.valueOf(template.getGUID().getUUID());
    log.debug("templateid is {}", templateid);
    return templateid;
  }

  /**
   * Fix up the base url. Replace an initial <code>../</code> with the <code>/Rhythmyx</code>.
   *
   * @param urlbase the base url.
   * @return the fixed base url.
   */
  protected String fixupUrl(String urlbase) {
    if (urlbase.startsWith("../")) {
      urlbase = urlbase.replace("../", "/Rhythmyx/");
    }
    return urlbase;
  }

  /** Default Assembler Url */
  protected static final String DEFAULT_ASSY_URL = "/Rhythmyx/assembler/render";

  /**
   * Returns the defaultLocationUrl.
   * @return the defaultLocationUrl
   */
  public String getDefaultLocationUrl() {
    return defaultLocationUrl;
  }

  /**
   * Sets the defaultLocationUrl.
   * @param defaultLocationUrl the defaultLocationUrl to set
   */
  public void setDefaultLocationUrl(String defaultLocationUrl) {
    this.defaultLocationUrl = defaultLocationUrl;
  }

  /**
   * Returns the multipleLocationUrl.
   * @return the multipleLocationUrl
   */
  public String getMultipleLocationUrl() {
    return multipleLocationUrl;
  }

  /**
   * Sets the multipleLocationUrl.
   * @param multipleLocationUrl the multipleLocationUrl to set
   */
  public void setMultipleLocationUrl(String multipleLocationUrl) {
    this.multipleLocationUrl = multipleLocationUrl;
  }
}
