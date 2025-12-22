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
package com.percussion.sitemanage.service.impl;

import static com.percussion.share.web.service.PSRestServicePathConstants.*;

import com.percussion.foldermanagement.service.IPSFolderService;
import com.percussion.itemmanagement.service.IPSItemService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSEnumVals;
import com.percussion.share.data.PSMapWrapper;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.sitemanage.data.*;
import com.percussion.sitemanage.error.PSSiteImportException;
import com.percussion.sitemanage.service.IPSSiteSectionService;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/site")
@PSSiteManageBean("siteDataRestService")
public class PSSiteDataRestService {
  private static final Logger log = LogManager.getLogger(PSSiteDataRestService.class);

  private final PSSiteDataService siteDataService;

  @Autowired
  public PSSiteDataRestService(PSSiteDataService siteDataService) {
    this.siteDataService = siteDataService;
  }

  @GET
  @Path(LOAD_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite load(@PathParam(ID_PATH_PARAM) String id) throws DataServiceLoadException {
    try {
      return siteDataService.load(id);
    } catch (IPSDataService.DataServiceNotFoundException | PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(404);
    }
  }

  @GET
  @Path(FIND_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSiteSummary find(@PathParam(ID_PATH_PARAM) String id)
      throws IPSDataService.DataServiceLoadException {
    try {
      return siteDataService.find(id);
    } catch (PSValidationException | IPSGenericDao.LoadException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Path(FIND_ALL_PATH)
  public PSSiteSummaryList findAll(@QueryParam("includePubInfo") boolean includePubInfo) {
    return new PSSiteSummaryList(siteDataService.findAll(includePubInfo));
  }

  @DELETE
  @Path(DELETE_PATH)
  public void delete(@PathParam(ID_PATH_PARAM) String id) {
    try {
      siteDataService.delete(id);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @POST
  @Path(SAVE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite save(PSSite site) throws PSParametersValidationException {
    try {
      return siteDataService.save(site);
    } catch (PSParametersValidationException pve) {
      throw pve;
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @POST
  @Path(IMPORT_SITE_FROM_URL_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite createSiteFromUrl(@Context HttpServletRequest request, PSSite site)
      throws PSSiteImportException {
    try {
      return siteDataService.createSiteFromUrl(request, site);
    } catch (PSValidationException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path(IMPORT_SITE_FROM_URL_PATH_ASYNC)
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public long createSiteFromUrlAsync(
      @Context HttpServletRequest request, PSSiteImportConfiguration site) {
    try {
      return siteDataService.createSiteFromUrlAsync(request, site);
    } catch (PSValidationException | IPSFolderService.PSWorkflowNotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path(GET_IMPORTED_SITE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite getImportedSite(@PathParam(JOB_ID_PARAM) Long jobId) {
    return siteDataService.getImportedSite(jobId);
  }

  @POST
  @Path(VALIDATE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSValidationErrors validate(PSSite site) {
    try {
      return siteDataService.validate(site);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @GET
  @Path("/properties/{siteName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSiteProperties getSiteProperties(@PathParam("siteName") String siteName) {
    try {
      return siteDataService.getSiteProperties(siteName);
    } catch (IPSSiteSectionService.PSSiteSectionException
        | PSValidationException
        | PSNotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/updateProperties")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSiteProperties updateSiteProperties(PSSiteProperties props) {
    try {
      return siteDataService.updateSiteProperties(props);
    } catch (PSNotFoundException | PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/publishProperties/{siteName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSitePublishProperties getSitePublishProperties(@PathParam("siteName") String siteName) {
    try {
      return siteDataService.getSitePublishProperties(siteName);
    } catch (PSValidationException | PSNotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/updatePublishProperties")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSitePublishProperties updateSitePublishProperties(PSSitePublishProperties publishProps) {
    try {
      return siteDataService.updateSitePublishProperties(publishProps);
    } catch (IPSDataService.DataServiceSaveException | PSNotFoundException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/choices")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEnumVals getChoices() {
    return siteDataService.getChoices();
  }

  @GET
  @Path("/copysiteinfo")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSMapWrapper getCopySiteInfo() {
    return siteDataService.getCopySiteInfo();
  }

  @GET
  @Path("/statistics/{siteId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSiteStatisticsSummary getSiteStatistics(@PathParam("siteId") String siteId) {
    try {
      return siteDataService.getSiteStatistics(siteId);
    } catch (PSDataServiceException e) {
      throw new WebApplicationException(e.getMessage());
    }
  }

  @GET
  @Path("/sass/sitenames")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.APPLICATION_XML})
  public PSMapWrapper getSaaSSiteNames(@QueryParam("filterUsedSites") boolean filterUsedSites) {
    try {
      return siteDataService.getSaaSSiteNames(filterUsedSites);
    } catch (DataServiceLoadException e) {
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/isSiteImporting/{sitename}")
  @Produces(MediaType.TEXT_PLAIN)
  public String isSiteBeingImported(@PathParam("sitename") String sitename) {
    try {
      return siteDataService.isSiteBeingImported(sitename);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @POST
  @Path("/validateFolders")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void validateFolders(PSValidateCopyFoldersRequest req) {
    try {
      siteDataService.validateFolders(req);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @POST
  @Path("/copy")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite copy(PSSiteCopyRequest req) {
    try {
      return siteDataService.copy(req);
    } catch (IPSItemService.PSItemServiceException | PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }
}
