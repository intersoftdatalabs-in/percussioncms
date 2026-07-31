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

package com.percussion.cloudservice.impl;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.percussion.cloudservice.IPSCloudService;
import com.percussion.cloudservice.data.PSCloudLicenseType;
import com.percussion.cloudservice.data.PSCloudServiceInfo;
import com.percussion.cloudservice.data.PSCloudServicePageData;
import com.percussion.licensemanagement.data.PSModuleLicense;
import com.percussion.licensemanagement.error.PSLicenseServiceException;
import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.server.PSServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Implementation of cloud service API for Percussion CMS. */
@Service("cloudService")
@Path("/cloudservice")
public class PSCloudService implements IPSCloudService {

  protected static final String PAGE_THUMB_ROOT = "/rx_resources/images/TemplateImages/";
  protected static final String PAGE_THUMB_SUFFIX = "-page.jpg";
  protected static final String CLOUD_SERVICE_TYPE_CM1 = "CM1";
  protected static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  protected IPSFolderHelper folderHelper;
  protected IPSRenderService renderService;
  protected IPSPageService pageService;
  protected PSLicenseService licenseService;
  protected boolean isLogged;
  protected static Logger log;

  @Autowired
  public PSCloudService(
      IPSFolderHelper folderHelper,
      IPSRenderService renderService,
      IPSPageService pageService,
      PSLicenseService licenseService) {
    this.folderHelper = folderHelper;
    this.renderService = renderService;
    this.pageService = pageService;
    this.licenseService = licenseService;
    log = LogManager.getLogger(PSCloudService.class);
  }

  @Override
  @GET
  @Path("/active")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean isActive() {
    return isValidLicense(PSCloudLicenseType.PAGE_OPTIMIZER);
  }

  @Override
  @GET
  @Path("/{licenseType}/active")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public boolean isActive(@PathParam("licenseType") PSCloudLicenseType licenseType) {
    return isValidLicense(licenseType);
  }

  @Override
  @GET
  @Path("/activestates")
  @Produces(MediaType.TEXT_PLAIN)
  public String getActiveState() {
    Map<String, Boolean> states = new LinkedHashMap<>();
    for (var type : PSCloudLicenseType.values()) {
      boolean valid = isValidLicense(type);
      states.put(type.toString(), valid);
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(states);
    } catch (Exception e) {
      log.error("Failed to serialize states", e);
      return "{}";
    }
  }

  @Override
  @GET
  @Path("/info")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCloudServiceInfo getInfo() throws PSCloudServiceException {
    PSModuleLicense poLic = null;
    try {
      poLic = getLicense(PSCloudLicenseType.PAGE_OPTIMIZER);
    } catch (PSLicenseServiceException ignored) {
    }
    if (poLic == null) {
      try {
        poLic = getLicense(PSCloudLicenseType.SOCIAL_PROMOTION);
      } catch (PSLicenseServiceException ignored) {
      }
    }
    if (poLic == null) {
      throw new PSCloudServiceException("No cloud services are enabled for this instance of CM1");
    }
    var info = new PSCloudServiceInfo();
    info.setClientIdentity(generateClientIdentity(poLic));
    info.setUiProvider(StringUtils.defaultString(poLic.getUiProvider().orElse(null)));
    return info;
  }

  @Override
  @GET
  @Path("/{licenseType}/info")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCloudServiceInfo getInfo(@PathParam("licenseType") PSCloudLicenseType licenseType) {
    PSModuleLicense poLic;
    try {
      poLic = getLicense(licenseType);
    } catch (PSLicenseServiceException le) {
      log.error(le.getMessage());
      log.debug(le.getMessage(), le);
      throw new WebApplicationException(
          licenseType.toFriendlyString() + " is not enabled for this instance of Percussion CMS");
    }
    var info = new PSCloudServiceInfo();
    info.setClientIdentity(generateClientIdentity(poLic));
    info.setUiProvider(StringUtils.defaultString(poLic.getUiProvider().orElse(null)));
    return info;
  }

  @Override
  @GET
  @Path("/pagedata/{pageId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCloudServicePageData getPageData(@PathParam("pageId") String pageId) {
    try {
      var info = getInfo();
      return getPageData(info, pageId);
    } catch (PSCloudServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @Override
  @GET
  @Path("/{licenseType}/pagedata/{pageId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSCloudServicePageData getPageData(
      @PathParam("licenseType") PSCloudLicenseType licenseType,
      @PathParam("pageId") String pageId) {
    try {
      var info = getInfo(licenseType);
      return getPageData(info, pageId);
    } catch (PSCloudServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(e.getMessage(), e);
      throw new WebApplicationException(e);
    }
  }

  @Override
  @POST
  @Path("/pagedata")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void savePageData(PSCloudServicePageData pageData) {
    var pageId = pageData.getId();
    try {
      var page = pageService.load(pageId);
      page.setTitle(pageData.getPageTitle());
      page.setDescription(pageData.getPageDescription());
      pageService.save(page);
    } catch (Throwable cause) {
      throw new WebApplicationException(cause);
    }
  }

  /**
   * Generates client identity as a JSON string.
   *
   * @param poLic the module license.
   * @return stringified JSON object of client identity.
   */
  public String generateClientIdentity(PSModuleLicense poLic) {
    Map<String, Object> ci = new LinkedHashMap<>();
    ci.put("id", poLic.getKey().orElse(""));
    ci.put("type", CLOUD_SERVICE_TYPE_CM1);
    ci.put("token", poLic.getHandshake().orElse(""));
    try {
      return OBJECT_MAPPER.writeValueAsString(ci);
    } catch (Exception e) {
      log.error("Failed to serialize client identity", e);
      return "{}";
    }
  }

  /**
   * Generates the thumbnail URL for a page.
   *
   * @param pageId the page ID.
   * @param siteName the site name.
   * @return the thumbnail URL for the supplied page.
   */
  public String generateThumbUrl(String pageId, String siteName) {
    // CWE-22: siteName/pageId are path segments under rxDir + PAGE_THUMB_ROOT
    PSPathInjectionGuard.requireSafeFileName(siteName);
    PSPathInjectionGuard.requireSafeFileName(pageId);
    var thumbUrl = PAGE_THUMB_ROOT + siteName + "/" + pageId + PAGE_THUMB_SUFFIX;
    File rx = PSServer.getRxDir();
    var file = PSPathInjectionGuard.requireUnderBase(rx, thumbUrl.replaceFirst("^/+", ""));
    if (!file.exists()) { // codeql[java/path-injection]
      thumbUrl = "";
    } else {
      thumbUrl = "/Rhythmyx" + thumbUrl;
    }
    return thumbUrl;
  }

  /** Determines whether cloud service is licensed for the given license type. */
  public boolean isValidLicense(PSCloudLicenseType licenseType) {
    PSModuleLicense poLic = null;
    try {
      poLic = getLicense(licenseType);
    } catch (PSLicenseServiceException le) {
      if (!isLogged) {
        isLogged = true;
        log.info(
            "{} is not enabled for this instance of CM1, activate the license using license monitor"
                + " gadget.",
            licenseType.toFriendlyString());
        log.error(PSExceptionUtils.getMessageForLog(le));
        log.debug(le);
      }
    }
    return poLic != null;
  }

  /**
   * Gets the license for the given license type.
   *
   * @param licenseType the module being licensed.
   * @return the module license.
   * @throws PSLicenseServiceException if license is not found.
   */
  public PSModuleLicense getLicense(PSCloudLicenseType licenseType)
      throws PSLicenseServiceException {
    var licType =
        switch (licenseType) {
          case PAGE_OPTIMIZER -> PSLicenseService.MODULE_LICENSE_TYPE_PAGE_OPTIMIZER;
          case SOCIAL_PROMOTION -> PSLicenseService.MODULE_LICENSE_TYPE_REDIRECT;
        };
    return licenseService.findModuleLicense(licType);
  }

  /** Builds page data from item properties and additional info. */
  private PSCloudServicePageData getPageData(PSCloudServiceInfo info, String pageId)
      throws PSCloudServiceException {
    PSItemProperties itemProps;
    String siteName;
    PSPage page;
    try {
      itemProps = folderHelper.findItemPropertiesById(pageId);
      List<IPSSite> sites = folderHelper.getItemSites(pageId);
      siteName = sites.get(0).getName();
      page = pageService.load(pageId);
    } catch (Throwable cause) {
      throw new PSCloudServiceException(cause);
    }
    var pageData = createFromItemProps(itemProps);
    pageData.setClientIdentity(info.getClientIdentity());
    pageData.setUiProviderUrl(info.getUiProvider());
    pageData.setThumbUrl(generateThumbUrl(pageId, siteName));
    pageData.setPageTitle(page.getTitle());
    pageData.setPageDescription(page.getDescription());
    return pageData;
  }

  /** Creates a PSCloudServicePageData object from item properties. */
  private PSCloudServicePageData createFromItemProps(PSItemProperties itemProps) {
    var pageId = itemProps.getId();
    var path = itemProps.getPath();
    var pageData = new PSCloudServicePageData();
    pageData.setId(pageId);
    pageData.setPath(path);

    var folders = path.split("/");
    var pagePath = new StringBuilder();
    for (int i = 3; i < folders.length; i++) {
      pagePath.append("/").append(folders[i]);
    }
    pageData.setPagePath(pagePath.toString());

    int index = path.lastIndexOf("/");
    if (path.length() > index) {
      pageData.setPageName(path.substring(path.lastIndexOf("/") + 1));
    } else {
      log.error("Failed to find the name for the page with path {}", path);
    }

    pageData.setLastPublished(itemProps.getLastPublishedDate());
    pageData.setStatus(itemProps.getStatus());
    pageData.setWorkflow(itemProps.getWorkflow());
    pageData.setLastEdited(itemProps.getLastModifiedDate());
    pageData.setLastPublished(itemProps.getLastPublishedDate());

    var renderedPage = renderService.renderPage(pageId);
    pageData.setPageHtml(renderedPage);

    return pageData;
  }
}
