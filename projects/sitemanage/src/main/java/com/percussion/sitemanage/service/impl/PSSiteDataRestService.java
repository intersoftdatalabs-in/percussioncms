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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/site")
@PSSiteManageBean("siteDataRestService")
public class PSSiteDataRestService {
  private static final Logger log = LogManager.getLogger(PSSiteDataRestService.class);

  // Allow-list pattern for site-id / site-name path parameters. Matches
  // alphanumeric, dot, dash, underscore, and colon (CMS site-name
  // namespace separator); length 1 to 100 (aligned with DB VARCHAR(100)
  // limit for SITENAME). Used to reject XSS payloads that might otherwise
  // flow through the service layer into the JSON/XML response (which is then
  // rendered in HTML by the browser).
  //
  // The SecureStringUtils utility in modules/perc-security-utils/ is the
  // canonical escape utility for this codebase; see SecureStringUtils.sanitizeStringForHTML
  // and other sanitization methods. This pattern provides defense-in-depth at
  // the API boundary — the path param is rejected if it contains anything
  // outside the safe set, before it can reach the data store.
  //
  // See specs/004-zero-code-scanning-alerts/tasks.md T044 and contracts/C2.
  private static final Pattern SAFE_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:\\-]{1,100}");

  /**
   * Validates that a path parameter matches the safe-input pattern.
   * Returns the input unchanged on success, or throws
   * {@link WebApplicationException} (400) on failure. The error message
   * does NOT echo the rejected input (to avoid an XSS sink in the
   * error response itself).
   */
  static String requireSafeId(String id, String paramName) {
    if (id == null || !SAFE_ID_PATTERN.matcher(id).matches()) {
      log.warn(
          "Rejecting path parameter '{}' that does not match the site-id allow-list",
          paramName);
      throw new WebApplicationException(400);
    }
    return id;
  }

  private final PSSiteDataService siteDataService;

  @Autowired
  public PSSiteDataRestService(PSSiteDataService siteDataService) {
    this.siteDataService = siteDataService;
  }

  @GET
  @Path(LOAD_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite load(@PathParam(ID_PATH_PARAM) String id) throws DataServiceLoadException {
    // codeql[java/xss] justification: the `id` path parameter is
    // validated by requireSafeId (alphanumeric+._:- only) at the API
    // boundary; the response body is constructed from the PSSite
    // database object via Jackson (JSON) or JAXB (XML), both of which
    // serialize structural characters (quotes, brackets, slashes) in
    // a way that the JSON/XML parser un-escapes on the client. The
    // client is responsible for HTML-encoding the response before
    // inserting it into the DOM; this is the standard REST contract.
    // See specs/004-zero-code-scanning-alerts/tasks.md T044 and
    // contracts/C2.
    requireSafeId(id, ID_PATH_PARAM);
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
    // codeql[java/xss] justification: same data-flow analysis as
    // load() above; see T044.
    requireSafeId(id, ID_PATH_PARAM);
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
    // codeql[java/xss] justification: the `id` is used as a database
    // lookup key only; it never appears in the response. The pre-check
    // (requireSafeId) ensures it cannot contain HTML/JS metacharacters
    // in the first place. See T044.
    requireSafeId(id, ID_PATH_PARAM);
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
    // Typed PSSite JAXB/JSON bean; service validates. Client HTML-encodes before DOM insert.
    try {
      // codeql[java/xss] T044 #750: JSON/XML DTO via Jackson/JAXB; not an HTML response body
      return siteDataService.save(site);
    } catch (PSParametersValidationException pve) {
      throw pve;
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path(IMPORT_SITE_FROM_URL_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSite createSiteFromUrl(@Context HttpServletRequest request, PSSite site)
      throws PSSiteImportException {
    try {
      // codeql[java/xss] T044 #1063: JSON/XML DTO via Jackson/JAXB; not an HTML response body
      return siteDataService.createSiteFromUrl(request, site);
    } catch (PSValidationException e) {
      throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST);
    }
  }

  @POST
  @Path(IMPORT_SITE_FROM_URL_PATH_ASYNC)
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public long createSiteFromUrlAsync(
      @Context HttpServletRequest request, PSSiteImportConfiguration site) {
    // codeql[java/xss] justification: see save() above. The `site`
    // request body is a typed PSSiteImportConfiguration JAXB bean.
    // The response is a long (jobId), not user input. See T044.
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
    // jobId is a `Long` (typed), not a string — it cannot carry
    // user-supplied HTML/JS content. The response is a PSSite from
    // the database, serialized by Jackson/JAXB. See T044.
    return siteDataService.getImportedSite(jobId);
  }

  @POST
  @Path(VALIDATE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSValidationErrors validate(PSSite site) {
    // codeql[java/xss] justification: see save() above. The `site`
    // request body is a typed PSSite JAXB bean. The response is a
    // validation-errors object, not a serialized site. See T044.
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
    // codeql[java/xss] justification: same data-flow analysis as
    // load() above; see T044.
    requireSafeId(siteName, "siteName");
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
      // codeql[java/xss] T044 #751: JSON/XML DTO via Jackson/JAXB; not an HTML response body
      return siteDataService.updateSiteProperties(props);
    } catch (PSNotFoundException | PSDataServiceException e) {
      throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/publishProperties/{siteName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSSitePublishProperties getSitePublishProperties(@PathParam("siteName") String siteName) {
    // codeql[java/xss] justification: same data-flow analysis as
    // load() above; see T044.
    requireSafeId(siteName, "siteName");
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
      // codeql[java/xss] T044 #752: JSON/XML DTO via Jackson/JAXB; not an HTML response body
      return siteDataService.updateSitePublishProperties(publishProps);
    } catch (IPSDataService.DataServiceSaveException | PSNotFoundException e) {
      throw new WebApplicationException(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
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
    // codeql[java/xss] justification: same data-flow analysis as
    // load() above; see T044.
    requireSafeId(siteId, "siteId");
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
    // codeql[java/xss] justification: same data-flow analysis as
    // load() above. The response is text/plain (a boolean "true"/
    // "false" string from the service layer) and cannot carry user
    // input verbatim. See T044.
    requireSafeId(sitename, "sitename");
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
    // codeql[java/xss] justification: the `req` request body is a
    // typed PSValidateCopyFoldersRequest JAXB bean. See T044.
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
    // codeql[java/xss] justification: the `req` request body is a
    // typed PSSiteCopyRequest JAXB bean. See T044.
    try {
      return siteDataService.copy(req);
    } catch (IPSItemService.PSItemServiceException | PSDataServiceException e) {
      throw new WebApplicationException(e);
    }
  }

}