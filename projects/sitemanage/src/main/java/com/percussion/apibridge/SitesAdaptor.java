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

package com.percussion.apibridge;

import com.percussion.cms.IPSConstants;
import com.percussion.rest.sites.ISiteAdaptor;
import com.percussion.rest.sites.Site;
import com.percussion.rest.sites.SiteList;
import com.percussion.rest.sites.VirtualSiteProperties;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.sitemgr.IPSPublishingContext;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.services.sitemgr.data.PSSite;
import com.percussion.services.virtualsite.PSVirtualSiteHelper;
import com.percussion.services.virtualsite.VirtualSiteException;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.publishing.IPSPublishingWs;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/** Adaptor for managing sites in Percussion CMS, including Virtual Site properties. */
@PSSiteManageBean
@Lazy
public class SitesAdaptor implements ISiteAdaptor {
  private static final Logger log = LogManager.getLogger(IPSConstants.API_LOG);

  /** Preferred default context name when creating new virtual.* properties. */
  static final String DEFAULT_PROPERTY_CONTEXT = "Preview";

  @Autowired private IPSPublishingWs publishingWs;

  @Autowired private IPSSiteDataService siteDataService;

  @Autowired private IPSSiteSectionService siteSectionService;

  private final IPSSiteManager siteManager;

  /** Default constructor (Spring / locator). */
  public SitesAdaptor() {
    this(PSSiteManagerLocator.getSiteManager());
  }

  /**
   * Test-friendly constructor.
   *
   * @param siteManager site manager, not null
   */
  public SitesAdaptor(IPSSiteManager siteManager) {
    this.siteManager = siteManager != null ? siteManager : PSSiteManagerLocator.getSiteManager();
  }

  @Override
  public SiteList findAllSites() {
    var sites = siteDataService.findAll();
    return ApiUtils.convertSiteSummaryList(sites);
  }

  @Override
  public void saveSite(Site site) {
    // General site save remains a later slice; use updateVirtualSiteProperties for virtual.*.
    throw new WebApplicationException(
        "General site save is not implemented; use PUT /sites/{nameOrId}/virtual for Virtual Site"
            + " properties",
        Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public Site findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    IPSSite site = siteManager.findSite(name.trim());
    return site == null ? null : toDetailSite(site);
  }

  @Override
  public Site findByGuid(String guid) {
    if (StringUtils.isBlank(guid)) {
      return null;
    }
    try {
      IPSGuid id = new PSGuid(guid.trim());
      IPSSite site = siteManager.findSite(id);
      return site == null ? null : toDetailSite(site);
    } catch (RuntimeException e) {
      log.debug("findByGuid: not a valid site guid '{}': {}", guid, e.getMessage());
      return null;
    }
  }

  @Override
  public void deleteSite(Site site) {
    throw new WebApplicationException(
        "Site delete is not implemented on this adaptor", Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public Site createSite() {
    throw new WebApplicationException(
        "Site create is not implemented on this adaptor", Response.Status.NOT_IMPLEMENTED);
  }

  @Override
  public VirtualSiteProperties getVirtualSiteProperties(String nameOrId) {
    IPSSite site = requireSite(nameOrId);
    return readVirtual(site);
  }

  @Override
  public VirtualSiteProperties updateVirtualSiteProperties(
      String nameOrId, VirtualSiteProperties props) {
    if (props == null) {
      throw new WebApplicationException(
          "VirtualSiteProperties body is required", Response.Status.BAD_REQUEST);
    }
    IPSSite found = requireSite(nameOrId);
    try {
      IPSSite modifiable = loadModifiable(found);
      if (!(modifiable instanceof PSSite psSite)) {
        throw new WebApplicationException(
            "Site entity does not support property bag", Response.Status.INTERNAL_SERVER_ERROR);
      }

      IPSGuid contextId = resolvePropertyContext(psSite);
      String sourceKind = blankToNull(props.getSourceKind().orElse(null));
      String rootPath = blankToNull(props.getRootPath().orElse(null));
      String configFile = blankToNull(props.getConfigFile().orElse(null));
      String siteKey = blankToNull(props.getSiteKey().orElse(null));

      // Clear virtual config when sourceKind is blank or explicit repository.
      if (sourceKind == null
          || PSVirtualSiteHelper.SOURCE_KIND_REPOSITORY.equalsIgnoreCase(sourceKind)) {
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SOURCE_KIND, null);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_ROOT_PATH, null);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_CONFIG_FILE, null);
        PSVirtualSiteHelper.putProperty(psSite, contextId, PSVirtualSiteHelper.PROP_SITE_KEY, null);
      } else {
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SOURCE_KIND, sourceKind);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_ROOT_PATH, rootPath);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_CONFIG_FILE, configFile);
        PSVirtualSiteHelper.putProperty(
            psSite, contextId, PSVirtualSiteHelper.PROP_SITE_KEY, siteKey);
      }

      try {
        PSVirtualSiteHelper.validate(psSite);
      } catch (VirtualSiteException e) {
        throw new WebApplicationException(e.getMessage(), Response.Status.BAD_REQUEST);
      }

      siteManager.saveSite(psSite);
      return readVirtual(psSite);
    } catch (WebApplicationException e) {
      throw e;
    } catch (PSNotFoundException e) {
      throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
    } catch (Exception e) {
      log.error(
          "Failed to update virtual site properties for '{}' ({}): {}",
          nameOrId,
          e.getClass().getName(),
          e.getMessage(),
          e);
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Map domain site to rest detail DTO including virtual properties.
   *
   * @param site domain site, not null
   * @return rest site
   */
  Site toDetailSite(IPSSite site) {
    Site ret = new Site();
    ret.setName(site.getName());
    ret.setDescription(site.getDescription());
    ret.setBaseUrl(site.getBaseUrl());
    ret.setDefaultFileExtention(site.getDefaultFileExtension());
    ret.setCanonical(site.isCanonical());
    ret.setCanonicalDist(site.getCanonicalDist());
    ret.setCanonicalReplace(site.isCanonicalReplace());
    ret.setDefaultDocument(site.getDefaultDocument());
    ret.setSiteProtocol(site.getSiteProtocol());
    ret.setOverrideSystemFoundation(site.isOverrideSystemFoundation());
    ret.setOverrideSystemJQuery(site.isOverrideSystemJQuery());
    ret.setOverrideSystemJQueryUI(site.isOverrideSystemJQueryUI());
    ret.setSiteAdditionalHeadContent(site.getSiteAdditionalHeadContent());
    ret.setSiteAfterBodyOpenContent(site.getSiteAfterBodyOpenContent());
    ret.setSiteBeforeBodyCloseContent(site.getSiteBeforeBodyCloseContent());
    if (site.getGUID() != null) {
      ret.setGuid(ApiUtils.convertGuid(site.getGUID()));
    }
    ret.setVirtual(readVirtual(site));
    return ret;
  }

  /**
   * Read virtual.* into the wire DTO.
   *
   * @param site domain site
   * @return never null
   */
  static VirtualSiteProperties readVirtual(IPSSite site) {
    VirtualSiteProperties v = new VirtualSiteProperties();
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SOURCE_KIND)
        .ifPresent(v::setSourceKind);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_ROOT_PATH)
        .ifPresent(v::setRootPath);
    // configFile: expose stored value only (not default) so clients see "unset" vs override
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_CONFIG_FILE)
        .ifPresent(v::setConfigFile);
    PSVirtualSiteHelper.findProperty(site, PSVirtualSiteHelper.PROP_SITE_KEY)
        .ifPresent(v::setSiteKey);
    v.setVirtual(PSVirtualSiteHelper.isVirtual(site));
    return v;
  }

  private IPSSite requireSite(String nameOrId) {
    if (StringUtils.isBlank(nameOrId)) {
      throw new WebApplicationException("nameOrId is required", Response.Status.BAD_REQUEST);
    }
    String key = nameOrId.trim();
    IPSSite site = siteManager.findSite(key);
    if (site != null) {
      return site;
    }
    try {
      site = siteManager.findSite(new PSGuid(key));
    } catch (RuntimeException e) {
      site = null;
    }
    if (site == null) {
      throw new WebApplicationException("Site not found: " + nameOrId, Response.Status.NOT_FOUND);
    }
    return site;
  }

  private IPSSite loadModifiable(IPSSite found) throws PSNotFoundException {
    if (found.getGUID() != null) {
      return siteManager.loadSiteModifiable(found.getGUID());
    }
    return siteManager.loadSiteModifiable(found.getName());
  }

  /**
   * Resolve context for new virtual.* properties: reuse existing property context when present,
   * else Preview (or first available publishing context).
   */
  IPSGuid resolvePropertyContext(PSSite site) throws PSNotFoundException {
    Optional<IPSGuid> existing =
        PSVirtualSiteHelper.findPropertyContext(site, PSVirtualSiteHelper.PROP_SOURCE_KIND);
    if (existing.isEmpty()) {
      existing =
          PSVirtualSiteHelper.findPropertyContext(site, PSVirtualSiteHelper.PROP_ROOT_PATH);
    }
    if (existing.isPresent()) {
      return existing.get();
    }
    try {
      IPSPublishingContext preview = siteManager.loadContext(DEFAULT_PROPERTY_CONTEXT);
      if (preview != null && preview.getGUID() != null) {
        return preview.getGUID();
      }
    } catch (PSNotFoundException e) {
      log.debug("Preview publishing context not found; falling back to first context");
    }
    List<IPSPublishingContext> contexts = siteManager.findAllContexts();
    if (contexts == null || contexts.isEmpty()) {
      throw new WebApplicationException(
          "No publishing context available to store virtual site properties",
          Response.Status.INTERNAL_SERVER_ERROR);
    }
    return contexts.get(0).getGUID();
  }

  private static String blankToNull(String value) {
    return StringUtils.isBlank(value) ? null : value.trim();
  }
}
