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
package com.percussion.pagemanagement.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.isTrue;

import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.data.PSTemplateSummaryList;
import com.percussion.pagemanagement.service.IPSTemplateService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSDataService.DataServiceSaveException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.web.service.PSRestServicePathConstants;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST service for template operations. Sunny Sal says: "REST easy, your templates are in good
 * hands!"
 */
@Path("/template")
@Component("templateRestService")
public class PSTemplateRestService {
  private final IPSTemplateService templateService;
  private static final Logger log = LogManager.getLogger(PSTemplateRestService.class);

  @Autowired
  public PSTemplateRestService(IPSTemplateService templateService) {
    this.templateService = templateService;
  }

  @GET
  @Path("/create/{name}/{srcId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSTemplateSummary createTemplate(
      @PathParam("name") String name, @PathParam("srcId") String srcId) {
    try {
      return templateService.createTemplate(name, srcId);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("id") String id) throws PSParametersValidationException {
    try {
      templateService.delete(id);
    } catch (PSParametersValidationException pve) {
      log.debug(pve.getMessage(), pve);
      throw pve;
    } catch (PSNotFoundException | PSDataServiceException e) {
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/summary/all")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSTemplateSummary> findAll() {
    try {
      return new PSTemplateSummaryList(templateService.findAll());
    } catch (IPSTemplateService.PSTemplateException | IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/summary/all/{siteName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSTemplateSummary> findAll(@PathParam("siteName") String siteName) {
    try {
      return new PSTemplateSummaryList(templateService.findAll(siteName));
    } catch (IPSTemplateService.PSTemplateException | IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/summary/all/user")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSTemplateSummary> findAllUserTemplates() {
    try {
      return new PSTemplateSummaryList(templateService.findAllUserTemplates());
    } catch (IPSTemplateService.PSTemplateException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/summary/all/readonly")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSTemplateSummary> findReadOnlyTemplates(@QueryParam("type") String type) {
    var baseType = StringUtils.isBlank(type) ? "base" : type;
    return new PSTemplateSummaryList(templateService.findBaseTemplates(baseType));
  }

  @GET
  @Path("/summary/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSTemplateSummary findTemplate(@PathParam("id") String id) {
    try {
      return templateService.find(id);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/loadTemplateMetadata/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSHtmlMetadata loadHtmlMetadata(@PathParam("id") String id) {
    try {
      return templateService.loadHtmlMetadata(id);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path("/saveTemplateMetadata")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void saveHtmlMetadata(PSHtmlMetadata object) {
    try {
      templateService.saveHtmlMetadata(object);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/templateEditUrl/{id}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getPageEditUrl(@PathParam("id") String id) {
    isTrue(isNotBlank(id), "id may not be blank");
    return templateService.getTemplateEditUrl(id);
  }

  @GET
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSTemplate load(@PathParam("id") String id) {
    try {
      var template = templateService.load(id);
      // Ensure XML elements for null-valued properties for JS update
      if (template.getHtmlHeader() == null) template.setHtmlHeader("");
      if (template.getDescription() == null) template.setDescription("");
      if (template.getImageThumbPath() == null) template.setImageThumbPath("");
      if (template.getLabel() == null) template.setLabel("");
      if (template.getTheme() == null) template.setTheme("");
      if (template.getCssOverride() == null) template.setCssOverride("");
      if (template.getCssRegion() == null) template.setCssRegion("");
      return template;
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path(PSRestServicePathConstants.SAVE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSTemplate save(PSTemplate object) {
    try {
      // JSON/XML DTO via Jackson/JAXB — not HTML body (see suppressions.md #1917/#1918)
      return templateService.save(object); // codeql[java/xss]
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }

  @POST
  @Path("/page/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSTemplate save(PSTemplate object, @PathParam("id") String pageId) {
    try {
      // JSON/XML DTO via Jackson/JAXB — not HTML body (see suppressions.md #1917/#1918)
      return templateService.save(object, null, pageId); // codeql[java/xss]
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @POST
  @Path(PSRestServicePathConstants.VALIDATE_PATH)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSValidationErrors validate(PSTemplate object) {
    try {
      return templateService.validate(object);
    } catch (PSValidationException | DataServiceSaveException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e.getMessage());
    }
  }
}
