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
package com.percussion.analytics.service.impl;

import com.percussion.analytics.data.PSAnalyticsProviderConfig;
import com.percussion.analytics.error.PSAnalyticsProviderException;
import com.percussion.analytics.error.PSAnalyticsProviderException.CAUSETYPE;
import com.percussion.analytics.service.IPSAnalyticsProviderService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSGAEntries;
import com.percussion.share.service.exception.PSValidationException;
import java.io.IOException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST service for analytics provider configuration and connection. Sunny Sal: "REST easy,
 * analytics is under control!"
 */
@Path("/provider")
@Component("analyticsProviderRestService")
public class PSAnalyticsProviderRestService {

  private static final Logger log = LogManager.getLogger(PSAnalyticsProviderRestService.class);
  private final IPSAnalyticsProviderService providerService;

  @Autowired
  public PSAnalyticsProviderRestService(IPSAnalyticsProviderService providerService) {
    this.providerService = providerService;
  }

  /** Gets analytics profiles. */
  @GET
  @Path("/profiles")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGAEntries getProfiles() throws PSAnalyticsProviderException, PSValidationException {
    try {
      var result = new PSGAEntries();
      result.setEntries(providerService.getProfiles(null, null));
      return result;
    } catch (IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Tests analytics provider connection. */
  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Path("/testConnection/{uid}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void testConnection(@PathParam("uid") String uid, @Multipart("file") Attachment attachment)
      throws PSAnalyticsProviderException, PSValidationException {
    try {
      String creds;
      try {
        creds = IOUtils.toString(attachment.getDataHandler().getInputStream());
      } catch (IOException e) {
        log.debug("Cannot parse .json key file", e);
        throw new PSAnalyticsProviderException(
            "Cannot parse .json key file", CAUSETYPE.INVALID_DATA);
      }
      providerService.testConnection(StringUtils.trimToEmpty(uid), creds);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } catch (IPSGenericDao.SaveException | IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Stores analytics provider config. */
  @POST
  @Path("/config")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void storeConfig(PSAnalyticsProviderConfig config) throws PSValidationException {
    try {
      providerService.saveConfig(config);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } catch (IPSGenericDao.SaveException | IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Gets stored analytics provider config. */
  @GET
  @Path("/config")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSAnalyticsProviderConfig getStoredConfig() throws PSValidationException {
    try {
      return providerService.loadConfig(true);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } catch (IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Deletes analytics provider config. */
  @DELETE
  @Path("/config")
  public void deleteConfig() {
    try {
      providerService.deleteConfig();
    } catch (IPSGenericDao.DeleteException | IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Checks if a profile is configured for the given site. */
  @GET
  @Path("/isProfileConfigured/{sitename}")
  @Produces(MediaType.TEXT_PLAIN)
  public String isProfileConfigured(@PathParam("sitename") String sitename)
      throws PSValidationException {
    try {
      return Boolean.toString(providerService.isProfileConfigured(sitename));
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw e;
    } catch (IPSGenericDao.LoadException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }
}
