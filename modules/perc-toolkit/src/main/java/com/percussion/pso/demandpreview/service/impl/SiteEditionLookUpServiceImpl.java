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

/*
 *
 */
package com.percussion.pso.demandpreview.service.impl;

import com.percussion.pso.demandpreview.exception.SiteLookUpException;
import com.percussion.pso.demandpreview.service.SiteEditionConfig;
import com.percussion.pso.demandpreview.service.SiteEditionHolder;
import com.percussion.pso.demandpreview.service.SiteEditionLookUpService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Site look up service implementation as a Spring bean.
 *
 * @author ravikatam
 */
public class SiteEditionLookUpServiceImpl implements SiteEditionLookUpService {
  private static final Logger log = LogManager.getLogger(SiteEditionLookUpServiceImpl.class);

  private IPSSiteManager siteManager = null;
  private IPSPublisherService publisherService = null;
  private IPSGuidManager guidManager = null;
  private IPSAssemblyService asm = null;
  private Map<String, SiteEditionConfig> siteLookUpMap;

  /**
   * Default Constructor
   * Creates a new SiteEditionLookUpServiceImpl.
   *
   */
  public SiteEditionLookUpServiceImpl() {}

  /**
   * Initialize the service bean. Must be called from Spring before any of the service methods.
   *
   * @throws Exception if an error occurs
   */
  public void init() throws Exception {
    if (siteManager == null) {
      siteManager = PSSiteManagerLocator.getSiteManager();
    }
    if (publisherService == null) {
      publisherService = PSPublisherServiceLocator.getPublisherService();
    }
    if (guidManager == null) {
      guidManager = PSGuidManagerLocator.getGuidMgr();
    }

    if (asm == null) {
      asm = PSAssemblyServiceLocator.getAssemblyService();
    }
  }

  /**
   * Gets the preview site, template, edition and context information from the configuration.
   *
   * @param siteId the site id to use as a lookup key.
   * @return the SiteTemplateHolder object. Never <code>null</code>
   * @throws SiteLookUpException when the site information cannot be located, or the names in the
   *     configuration are invalid.
   */
  public SiteEditionHolder LookUpSiteEdition(String siteId) throws SiteLookUpException {
    String emsg;
    if (StringUtils.isBlank(siteId)) {
      emsg = "No site id was specified";
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }
    IPSGuid siteGuid = guidManager.makeGuid(Integer.parseInt(siteId), PSTypeEnum.SITE);

    return LookUpSiteEdition(siteGuid);
  }

  /**
   * LookUpSiteEdition operation.
   *
   * @param siteId the site id
   * @return the result
   * @throws SiteLookUpException if an error occurs
   */
  public SiteEditionHolder LookUpSiteEdition(IPSGuid siteId) throws SiteLookUpException {
    String emsg;
    String pSiteName = null;
    String editionName = null;
    String siteName = null;
    SiteEditionConfig siteConfig = null;
    SiteEditionHolder siteEditionHolder = new SiteEditionHolder();

    IPSSite site;
    try {
      site = siteManager.loadSite(siteId);
    } catch (PSNotFoundException ex) {
      emsg = "Site Not Found for Id " + siteId;
      log.error(emsg, ex);
      throw new SiteLookUpException(emsg, ex);
    }
    siteName = site.getName();

    siteConfig = siteLookUpMap.get(siteName);
    if (siteConfig == null) {
      emsg = "Site not found in configuration: " + siteName;
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }

    pSiteName = siteConfig.getSiteName();
    editionName = siteConfig.getEditionName();
    if (StringUtils.isBlank(pSiteName)) {
      emsg = "Preview site not configured for " + siteName;
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }
    // Store the context variable
    siteEditionHolder.setContextURLRootVar(siteConfig.getContextURLRootVar());

    IPSSite pSite = null;
    try {
      pSite = siteManager.loadSite(pSiteName);
    } catch (PSNotFoundException e) {
      log.error("Unable to load Site: {}", pSiteName);
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new SiteLookUpException(e);
    }
    siteEditionHolder.setSite(pSite);
    if (editionName == null || editionName.equals("")) {
      emsg = "Edition not configured for " + siteName;
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }
    IPSEdition sEdition = getEdition(editionName);
    siteEditionHolder.setEdition(sEdition);

    IPSPublishingContext ctx = null;
    try {
      IPSGuid contextGuid =
          guidManager.makeGuid(siteConfig.getAssemblyContext(), PSTypeEnum.CONTEXT);
      ctx = siteManager.loadContext(contextGuid);
    } catch (PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    if (ctx == null) {
      emsg = "Context " + siteConfig.getAssemblyContext() + " not configured for " + siteName;
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }

    siteEditionHolder.setContext(ctx);

    return siteEditionHolder;
  }

  /**
   * Gets the set of sites for which there are configuration entries. *
   *
   * @return the set of names. Never <code>null</code> but may be <code>empty</code>
   */
  public Set<String> getConfiguredSiteNames() {
    Set<String> names = new LinkedHashSet<String>();
    names.addAll(siteLookUpMap.keySet());
    return names;
  }

  /**
   * Gets the edition
   *
   * @param editionName the edition name
   * @return the edition. Never <code>null</code>
   * @throws SiteLookUpException if an error occurs
   */
  protected IPSEdition getEdition(String editionName) throws SiteLookUpException {
    String emsg;
    IPSEdition edition = null;
    edition = publisherService.findEditionByName(editionName);
    if (edition == null) {
      emsg = "Edition not found: " + editionName;
      log.error(emsg);
      throw new SiteLookUpException(emsg);
    }
    return edition;
  }

  /**
   * Gets the site look up map instance
   *
   * @return the siteLookUpMap
   */
  public Map<String, SiteEditionConfig> getSiteLookUpMap() {
    return siteLookUpMap;
  }

  /**
   * Sets the site look up map instance.
   *
   * @param siteLookUpMap the site look up map
   */
  public void setSiteLookUpMap(Map<String, SiteEditionConfig> siteLookUpMap) {
    this.siteLookUpMap = siteLookUpMap;
  }

  /**
   * This method is only for testing purpose.
   *
   * @param siteManager the site manager
   */
  protected void setSiteManager(IPSSiteManager siteManager) {
    this.siteManager = siteManager;
  }

  /**
   * Sets the publisher service. This method is only for testing purpose.
   *
   * @param publisherService the publisher service
   */
  protected void setPubisherService(IPSPublisherService publisherService) {
    this.publisherService = publisherService;
  }

  /**
   * This method is only for testing purpose.
   *
   * @param guidManager the guid manager
   */
  protected void setGuidManager(IPSGuidManager guidManager) {
    this.guidManager = guidManager;
  }

  /**
   * This method is only for testing purpose.
   *
   * @param asm the asm to set
   */
  protected void setAsm(IPSAssemblyService asm) {
    this.asm = asm;
  }
}
