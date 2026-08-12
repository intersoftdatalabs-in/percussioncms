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
package com.percussion.pageoptimizer.impl;

import com.percussion.cloudservice.data.PSCloudLicenseType;
import com.percussion.cloudservice.impl.PSCloudService;
import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.pageoptimizer.IPSPageOptimizerService;
import com.percussion.pageoptimizer.data.PSPageOptimizerData;
import com.percussion.pageoptimizer.data.PSPageOptimizerInfo;
import com.percussion.share.dao.IPSFolderHelper;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Implementation of the Page Optimizer Service. Sunny Sal says: "Optimize first, ask questions
 * later!"
 */
@Path("/pageoptimizer")
@Component("pageOptimizerService")
@Lazy
public class PSPageOptimizerService extends PSCloudService implements IPSPageOptimizerService {

  @Autowired
  public PSPageOptimizerService(
      IPSFolderHelper folderHelper,
      IPSRenderService renderService,
      IPSPageService pageService,
      PSLicenseService licenseService) {
    super(folderHelper, renderService, pageService, licenseService);
    PSCloudService.log = LogManager.getLogger(PSPageOptimizerService.class);
  }

  /**
   * Checks if the Page Optimizer is active.
   *
   * @return true if active, false otherwise
   */
  @Override
  @GET
  @Path("/active")
  @Produces(MediaType.TEXT_PLAIN)
  public boolean isPageOptimizerActive() {
    return isValidLicense(PSCloudLicenseType.PAGE_OPTIMIZER);
  }

  /**
   * Gets the properties of the Page Optimizer service.
   *
   * @return PSPageOptimizerInfo, never null
   */
  @Override
  @GET
  @Path("/info")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPageOptimizerInfo getPageOptimizerInfo() {
    var cloudInfo = super.getInfo(PSCloudLicenseType.PAGE_OPTIMIZER);
    return PSPageOptimizerInfo.fromPSCloudServiceInfo(cloudInfo);
  }

  /**
   * Collects the data and consolidates them into a PSPageOptimizerData object.
   *
   * @param pageId must be a valid page id, string form of guid
   * @return PSPageOptimizerData, never null
   */
  @Override
  @GET
  @Path("/pagedata/{pageId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPageOptimizerData getPageOptimizerData(@PathParam("pageId") String pageId) {
    var cloudData = super.getPageData(PSCloudLicenseType.PAGE_OPTIMIZER, pageId);
    return PSPageOptimizerData.fromPSCloudServicePageData(cloudData);
  }
}
