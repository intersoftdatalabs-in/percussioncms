// REFACTORED: CP-JAVA11
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

package com.percussion.redirect.service.impl;

import com.percussion.licensemanagement.data.PSModuleLicense;
import com.percussion.licensemanagement.service.IPSLicenseService;
import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.redirect.data.PSCreateRedirectRequest;
import com.percussion.redirect.data.PSRedirectStatus;
import com.percussion.redirect.data.PSRedirectValidationData;
import com.percussion.redirect.data.PSRedirectValidationData.RedirectPathType;
import com.percussion.redirect.data.PSRedirectValidationResponse;
import com.percussion.redirect.data.PSRedirectValidationResponse.RedirectValidationStatus;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSitePublishStatusService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.service.IPSUtilityService;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Implementation of {@link IPSRedirectService} for managing redirects and validation. */
@Service("pSRedirectService")
@Deprecated
public class PSRedirectService implements IPSRedirectService {

  private static final Logger log = LogManager.getLogger(PSRedirectService.class);

  @Autowired private IPSLicenseService licenseService;

  @Autowired private IPSFolderHelper folderHelper;

  @Autowired private IPSSitePublishStatusService pubStatusService;

  @Autowired private IPSUtilityService utilityService;

  @Autowired private IPSSiteDataService siteDataService;

  @Autowired private IPSGuidManager guidMgr;

  @Override
  public PSRedirectValidationResponse validate(PSRedirectValidationData data) {
    var response = new PSRedirectValidationResponse();
    validateInput(data);

    var licInfo = getLicense();
    if (licInfo == null) {
      response.setStatus(RedirectValidationStatus.NO_LICENSE);
      return response;
    }
    response.setRedirectLicense(licInfo);
    RedirectValidationStatus status;
    try {
      var path =
          StringUtils.isBlank(data.getToPath())
              ? StringUtils.defaultString(data.getFromPath())
              : StringUtils.defaultString(data.getToPath());
      if (path.startsWith("/Sites/")) {
        path = "/" + path;
      }
      var sitename = getSiteNameFromPath(path);
      var site = siteDataService.find(sitename, true);
      site.getPubInfo().ifPresent(p -> response.setBucketName(p.getBucketName()));
      status = validatePathStatus(path, data.getType(), site);
    } catch (Exception e) {
      status = RedirectValidationStatus.ERROR;
      log.error(
          "Error occurred validating the redirect object, data: {}, Error: {}",
          getDataAsString(data),
          PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      response.setErrorMessage(
          "A redirect rule cannot be created at this time. Please note the time and date and submit"
              + " this problem to Percussion support.");
    }
    response.setStatus(status);
    return response;
  }

  private void validateInput(PSRedirectValidationData data) {
    if (StringUtils.isBlank(data.getFromPath())) {
      throw new IllegalArgumentException("fromPath must not be blank");
    }
    var fpath = data.getFromPath();
    if (!(fpath.startsWith("/Sites/") || fpath.startsWith("//Sites/"))) {
      throw new IllegalArgumentException("fromPath must start with either /Sites/ or //Sites/");
    }
    if (StringUtils.isNotBlank(data.getToPath())) {
      var tpath = data.getToPath();
      if (!(tpath.startsWith("/Sites/") || tpath.startsWith("//Sites/"))) {
        throw new IllegalArgumentException("toPath must start with either /Sites/ or //Sites/");
      }
    }
    if (data.getType() == null) {
      throw new IllegalArgumentException("type must not be null");
    }
  }

  private String getDataAsString(PSRedirectValidationData data) {
    var temp = "";
    var mapper = JsonMapper.builder().build();
    try {
      temp = mapper.writeValueAsString(data);
    } catch (Exception e) {
      temp =
          "{\"fromPath\":\"" + data.getFromPath() + "\", \"toPath\":\"" + data.getToPath() + "\"}";
    }
    return temp;
  }

  private String getSiteNameFromPath(String path) {
    path = path.replace("//Sites/", "");
    var paths = path.split("/");
    return paths[0];
  }

  @Override
  public PSModuleLicense getLicense() {
    PSModuleLicense licInfo = null;
    try {
      licInfo = licenseService.findModuleLicense(PSLicenseService.MODULE_LICENSE_TYPE_REDIRECT);
    } catch (Exception e) {
      // If there is no redirect license, we don't have to log it for on-prem customers.
      if (utilityService.isSaaSEnvironment()) {
        log.error("Error: {}", PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
    }
    return licInfo;
  }

  private RedirectValidationStatus validatePathStatus(
      String path, RedirectPathType type, PSSiteSummary site)
      throws DataServiceLoadException, Exception {
    if (path.startsWith("/Sites")) {
      path = "/" + path;
    }
    RedirectValidationStatus status = null;
    var isSitePublished = isSitePublished(site.getSiteId() + "");
    if (type == RedirectPathType.PAGE) {
      var props = folderHelper.findItemProperties(path);
      status =
          StringUtils.isBlank(props.getLastPublishedDate())
              ? RedirectValidationStatus.NOT_PUBLISHED
              : RedirectValidationStatus.PUBLISHED;
    } else if (type == RedirectPathType.SITE || type == RedirectPathType.SECTION) {
      status =
          isSitePublished
              ? RedirectValidationStatus.PUBLISHED
              : RedirectValidationStatus.NOT_PUBLISHED;
    } else if (type == RedirectPathType.FOLDER) {
      status = isSitePublished ? getFolderStatus(path) : RedirectValidationStatus.NOT_PUBLISHED;
    }
    return status;
  }

  private boolean isSitePublished(String siteId) throws PSDataServiceException {
    IPSGuid siteGuid = guidMgr.makeGuid(siteId, PSTypeEnum.SITE);
    return pubStatusService.isSitePublished(siteGuid);
  }

  private RedirectValidationStatus getFolderStatus(String path)
      throws DataServiceLoadException, Exception {
    return folderHelper.findItems(path).isEmpty()
        ? RedirectValidationStatus.NO_CHILDREN
        : RedirectValidationStatus.PUBLISHED;
  }

  @Override
  public PSRedirectStatus createRedirect(PSCreateRedirectRequest request) {
    // Assume failure
    var ret = new PSRedirectStatus(PSRedirectStatus.SERVICE_ERROR, "Error.");

    var client = ClientBuilder.newBuilder().newClient();
    var lic = getLicense();

    if (lic != null) {
      Response response = null;
      try {
        var target = client.target(lic.getApiProvider() + "/rest/redirect").path("entries");
        var builder =
            target
                .request(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .header(
                    "Authorization",
                    "{\"id\":\""
                        + lic.getKey()
                        + "\",\"type\":\""
                        + lic.getName()
                        + "\",\"token\":\""
                        + lic.getHandshake()
                        + "\"}");
        response = builder.get();
      } catch (Exception e) {
        log.error(
            "Error creating redirect from {}, to {}, Error: {}",
            request.getCondition(),
            request.getRedirectTo(),
            PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
      if (response != null && response.getStatus() == 200) {
        ret.setStatusCode(PSRedirectStatus.SERVICE_OK);
        ret.setMessage("Redirect created successfully.");
      } else {
        if (response != null) {
          ret.setMessage(
              "An error occurred while saving the redirect. Remote service returned status code: "
                  + response.getStatus());
        } else {
          ret.setMessage(
              "An error occurred while saving the redirect. Remote service returned status code:"
                  + " null");
        }
      }
    }
    return ret;
  }

  @Override
  public PSRedirectStatus status() {
    var ret = new PSRedirectStatus(PSRedirectStatus.SERVICE_ERROR, "Error");
    try {
      var lic =
          this.licenseService.findModuleLicense(PSLicenseService.MODULE_LICENSE_TYPE_REDIRECT);
      ret.setStatusCode(PSRedirectStatus.SERVICE_OK);
      ret.setMessage(lic.getName() + " is licensed and available.");
    } catch (Exception e) {
      ret.setStatusCode(PSRedirectStatus.SERVICE_UNLICENSED);
      ret.setMessage("Unable to locate an activated Redirect Service license.");
    }
    return ret;
  }
}
