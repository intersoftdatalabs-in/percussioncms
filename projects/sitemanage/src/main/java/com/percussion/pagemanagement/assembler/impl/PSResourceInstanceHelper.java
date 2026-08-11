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
package com.percussion.pagemanagement.assembler.impl;

import static com.percussion.pagemanagement.assembler.PSResourceLinkAndLocationUtils.concatPath;
import static com.percussion.pagemanagement.assembler.PSResourceLinkAndLocationUtils.createDefaultLinkAndLocation;
import static com.percussion.pagemanagement.assembler.PSResourceLinkAndLocationUtils.validateAsPhysicalPath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.pagemanagement.assembler.PSResourceScriptEvaluatorContext;
import com.percussion.pagemanagement.data.PSRenderLinkContext;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceInstance;
import com.percussion.pagemanagement.data.PSResourceLinkAndLocation;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.impl.PSLinkableAsset;
import com.percussion.pagemanagement.service.impl.PSRenderLinkService;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.utils.jexl.PSServiceJexlEvaluatorBase;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.data.IPSLinkableContentItem;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.IPSLinkableItem;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSiteTemplateService;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.jexl.PSJexlEvaluator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Helper to process {@link PSResourceInstance}s. Right now just for link and locations.
 *
 * <p>Its responsibilities are to create resource instances and manage executing them (getting links
 * and one day output).
 *
 * <p>The {@link PSRenderLinkService} delegates to this class even though publicly it is the one
 * responsible for resource instances.
 *
 * @author adamgent
 */
@PSSiteManageBean("resourceInstanceHelper")
public class PSResourceInstanceHelper {

  private IPSSiteDataService siteDataService;
  private IPSPageService pageService;
  private IPSSiteTemplateService siteTemplateService;
  private IPSAssetService assetService;
  private IPSAssemblyService assemblyService;

  @Autowired
  private PSResourceInstanceHelper(
      IPSAssetService assetService,
      IPSSiteDataService siteDataService,
      IPSPageService pageService,
      IPSSiteTemplateService siteTemplateService,
      IPSAssemblyService assemblyService) {
    super();
    this.assetService = assetService;
    this.siteDataService = siteDataService;
    this.pageService = pageService;
    this.siteTemplateService = siteTemplateService;
    this.assemblyService = assemblyService;
  }

  private List<PSResourceLinkAndLocation> executeResourceLinkScript(
      PSResourceInstance resourceInstance, String script)
      throws IPSAssetService.PSAssetServiceException {
    notNull(resourceInstance, "resourceInstance");
    notEmpty(script, "script");
    if (log.isDebugEnabled()) {
      log.debug("Executing Script: {} for resourceInstance: {}", script, resourceInstance);
    }
    var jexlEvaluator = new PSServiceJexlEvaluatorBase(true);
    try {
      var perc = createContext(resourceInstance);
      jexlEvaluator.bind("$perc", perc);
      var jexlScript = PSJexlEvaluator.createScript(script);
      var rvalue = jexlEvaluator.evaluate(jexlScript);
      if (rvalue instanceof List<?> links) {
        // verify list elements are of expected type since Validate no longer provides the helper
        var typed = new ArrayList<PSResourceLinkAndLocation>(links.size());
        for (Object o : links) {
          if (!(o instanceof PSResourceLinkAndLocation link)) {
            throw new IllegalArgumentException("Script returned invalid element: " + o);
          }
          typed.add(link);
        }
        return typed;
      } else if (rvalue instanceof PSResourceLinkAndLocation) {
        var links = new ArrayList<PSResourceLinkAndLocation>();
        links.add((PSResourceLinkAndLocation) rvalue);
        return links;
      } else {
        if (log.isDebugEnabled()) {
          log.debug(
              "Script did not return an object of type : {}",
              PSResourceLinkAndLocation.class.getSimpleName());
        }
      }
      return resourceInstance.getLinkAndLocations();
    } catch (Exception e) {
      throw new IPSAssetService.PSAssetServiceException(
          "Error executing link script for resource instance: " + resourceInstance, e);
    }
  }

  private PSResourceScriptEvaluatorContext createContext(PSResourceInstance resourceInstance) {
    var perc = new PSResourceScriptEvaluatorContext();
    perc.setResourceInstance(resourceInstance);
    return perc;
  }

  private String getPublishLocationFolderPath(PSResourceInstance r)
      throws DataServiceNotFoundException, PSValidationException {
    notNull(r, "r");
    var path = r.getItem().getFolderPath();
    path = path == null ? r.getLinkContext().getFolderPath() : path;
    var site = resolveSite(r);
    var siteFolderPath = site.getFolderPath();
    if (startsWith(path, siteFolderPath)) {
      path = removeStart(path, siteFolderPath);
    } else if (startsWith(path, PSAssetPathItemService.ASSET_ROOT)) {
      path = removeStart(path, PSAssetPathItemService.ASSET_ROOT);
      path = concatPath(PSPathUtils.ASSETS_FINDER_ROOT, path);
    } else {
      throw new RuntimeException(
          "The asset or link context associated with "
              + "the resource instance does not have a proper folder path: "
              + r);
    }
    if (isBlank(path)) {
      path = "/";
    }
    validateAsPhysicalPath(path);
    return path;
  }

  public String getBaseUrl(PSResourceInstance r) {
    var site = r.getSite();
    if (r.isCrossSite()) {
      // unwrap optional base url, empty string if missing
      return site.getBaseUrl().orElse("");
    }
    return getBaseUrlPath(r);
  }

  private PSSiteSummary resolveSite(PSResourceInstance r)
      throws DataServiceNotFoundException, PSValidationException {
    if (r.getSite() != null) {
      return r.getSite();
    }
    var site = r.getLinkContext().getSite();
    boolean itemIsResource = false;
    if (r.getItem() instanceof PSLinkableAsset asset) {
      itemIsResource = asset.isResource();
    } else if (r.getItem() instanceof com.percussion.share.data.PSDataItemSummary sum) {
      itemIsResource = sum.isResource();
    }
    if (!itemIsResource) {
      site = siteDataService.findByPath(r.getItem().getFolderPath());
    }
    notNull(site, "Either the link context or the item needs to belong to a site");
    return site;
  }

  public PSResourceInstance createResourceInstance(
      PSRenderLinkContext context, IPSLinkableItem item, PSAssetResource rd)
      throws IPSAssetService.PSAssetServiceException,
          DataServiceNotFoundException,
          PSValidationException {
    var r = new PSResourceInstance();
    if (item instanceof IPSLinkableContentItem) {
      r.setItem((IPSLinkableContentItem) item);
    } else {
      r.setItem(getLinkableItem(item.getId(), item.getFolderPath()));
    }
    r.setLinkContext(context);
    r.setResourceDefinition(rd);
    var site = resolveSite(r);
    r.setSite(site);
    r.setLocationFolderPath(getPublishLocationFolderPath(r));
    return r;
  }

  private IPSLinkableContentItem getLinkableItem(String assetId, String folderPath)
      throws IPSAssetService.PSAssetServiceException {
    var asset = loadPartialAsset(assetId);
    return new PSLinkableAsset(asset, folderPath);
  }

  public PSAsset loadPartialAsset(String assetId) throws IPSAssetService.PSAssetServiceException {
    return assetService.load(assetId, true);
  }

  public IPSItemSummary findResourceAsset(String assetId) throws PSDataServiceException {
    return assetService.find(assetId);
  }

  public List<PSResourceLinkAndLocation> getLinkAndLocations(PSResourceInstance r)
      throws IPSAssetService.PSAssetServiceException {
    var rd = r.getResourceDefinition();
    var script = rd.getLinkAndLocationsScript();
    if (script != null && isNotBlank(script.getValue())) {
      var links = executeResourceLinkScript(r, script.getValue());
      if (links.isEmpty()) {
        log.debug("Resource script returned an empty set of links");
      }
      return links;
    } else if (IPSPageService.PAGE_CONTENT_TYPE.equals(r.getItem().getType())) {
      log.debug("Generating link for page.");
      var link = createDefaultLinkAndLocation(r, assemblyService);
      return asList(link);
    }
    log.warn("Resource definition: {} does not generate any links.", r.getResourceDefinition());
    return emptyList();
  }

  public String getBaseUrlPath(PSRenderLinkContext context) {
    var site = context.getSite();
    return getBaseUrlPath(site);
  }

  private String getBaseUrlPath(PSResourceInstance r) {
    return getBaseUrlPath(r.getSite());
  }

  private String getBaseUrlPath(PSSiteSummary site) {
    var bu = site.getBaseUrl().orElse("");
    try {
      bu = new URL(bu).getPath();
    } catch (MalformedURLException e) {
      bu = "/";
    }
    return bu;
  }

  public PSSiteSummary getSiteForPageId(String pageId)
      throws IPSDataService.DataServiceLoadException,
          DataServiceNotFoundException,
          PSValidationException {
    var page = pageService.find(pageId);
    return siteDataService.findByPath(page.getFolderPath());
  }

  public PSSiteSummary getSiteForTemplateId(String templateId) {
    var sites = siteTemplateService.findSitesByTemplate(templateId);
    notEmpty(sites, "Template should be associated with at least one site");
    return sites.get(0);
  }

  private static final Logger log = LogManager.getLogger(PSResourceInstanceHelper.class);
}
