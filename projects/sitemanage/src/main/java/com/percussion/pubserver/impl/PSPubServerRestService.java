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
package com.percussion.pubserver.impl;

import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.delivery.service.PSDeliveryInfoServiceLocator;
import com.percussion.delivery.service.impl.PSDeliveryInfoService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.pubserver.data.PSPublishServerInfo;
import com.percussion.pubserver.data.PSPublishServerInfoList;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSParameterValidationUtils;
import com.percussion.share.service.exception.PSParametersValidationException;
import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import tools.jackson.databind.json.JsonMapper;

/**
 * REST endpoint for managing publishing servers.
 *
 * @author leonardohildt
 * @author ignacioerro
 */
@Path("/servers")
@Component("pubServerRestService")
@Lazy
public class PSPubServerRestService {
  private static final Logger log = LogManager.getLogger(PSPubServerRestService.class);

  private final IPSPubServerService service;

  @Autowired
  public PSPubServerRestService(IPSPubServerService service) {
    this.service = service;
  }

  /** Loads the server information based on the server name as the parameter. */
  @GET
  @Path("/{siteId}/{serverId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPublishServerInfo getPubServer(
      @PathParam("siteId") String siteId, @PathParam("serverId") String serverId) {
    try {
      return service.getPubServer(siteId, serverId);
    } catch (IPSPubServerService.PSPubServerServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Gets all publishing servers for a site. */
  @GET
  @Path("/{siteId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSPublishServerInfo> getServers(@PathParam("siteId") String siteId) {
    try {
      return new PSPublishServerInfoList(service.getPubServerList(siteId));
    } catch (IPSPubServerService.PSPubServerServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Creates a new publishing server. */
  @POST
  @Path("/{siteId}/{serverName:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPublishServerInfo createPubServer(
      @PathParam("siteId") String siteId,
      @PathParam("serverName") String serverName,
      PSPublishServerInfo pubServerInfo)
      throws PSValidationException, WebApplicationException {
    try {
      PSParameterValidationUtils.rejectIfBlank("create", "siteId", siteId);
      PSParameterValidationUtils.rejectIfBlank("create", "serverName", serverName);
      return service.createPubServer(siteId, serverName, pubServerInfo);
    } catch (PSParametersValidationException ps) {
      log.error(ps.getMessage());
      log.debug(ps.getMessage(), ps);
      throw ps;
    } catch (PSNotFoundException | IPSPubServerService.PSPubServerServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Updates an existing publishing server. */
  @PUT
  @Path("/{siteId}/{serverId:.*}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSPublishServerInfo updatePubServer(
      @PathParam("siteId") String siteId,
      @PathParam("serverId") String serverId,
      PSPublishServerInfo pubServerInfo)
      throws PSParametersValidationException {
    try {
      PSParameterValidationUtils.rejectIfBlank("update", "siteId", siteId);
      PSParameterValidationUtils.rejectIfBlank("update", "serverId", serverId);
      return service.updatePubServer(siteId, serverId, pubServerInfo);
    } catch (PSParametersValidationException ps) {
      log.error(ps.getMessage());
      log.debug(ps.getMessage(), ps);
      throw ps;
    } catch (PSDataServiceException
        | PSNotFoundException
        | IPSPubServerService.PSPubServerServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Deletes a publishing server. */
  @DELETE
  @Path("/{siteId}/{serverId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public List<PSPublishServerInfo> deleteServer(
      @PathParam("siteId") String siteId, @PathParam("serverId") String serverId) {
    try {
      PSParameterValidationUtils.rejectIfBlank("delete", "siteName", siteId);
      PSParameterValidationUtils.rejectIfBlank("delete", "serverId", serverId);
      return new PSPublishServerInfoList(service.deleteServer(siteId, serverId));
    } catch (IPSPubServerService.PSPubServerServiceException
        | PSDataServiceException
        | PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Stops a publishing job. */
  @POST
  @Path("/stopPublishing/{jobId}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public void stopPublishing(@PathParam("jobId") String jobId) {
    try {
      PSParameterValidationUtils.rejectIfBlank("delete", "jobId", jobId);
      service.stopPublishing(jobId);
    } catch (PSValidationException | IPSPubServerService.PSPubServerServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Gets information about available drivers. */
  @GET
  @Path("/availableDrivers")
  @Produces(MediaType.TEXT_PLAIN)
  public String getAvailableDrivers() throws tools.jackson.core.JacksonException {
    return JsonMapper.builder().build().writeValueAsString(service.getAvailableDrivers());
  }

  /** Gets available EC2 bucket regions. */
  @GET
  @Path("/availableRegions")
  @Produces(MediaType.TEXT_PLAIN)
  public String getAvailableRegions() throws tools.jackson.core.JacksonException {
    // v2 SDK equivalent of v1's Regions.values(): all known AWS partition regions.
    var regions = Region.regions();
    if (regions != null && !regions.isEmpty()) {
      var regionNames = new String[regions.size()];
      for (int i = 0; i < regions.size(); i++) {
        regionNames[i] = regions.get(i).id();
      }
      return JsonMapper.builder().build().writeValueAsString(regionNames);
    }
    return null;
  }

  /** Gets available publishing servers for a given type. */
  @GET
  @Path("/availablePublishingServer/{publishServerType}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getAvailablePublishingServer(@PathParam("publishServerType") String publishServer)
      throws tools.jackson.core.JacksonException {
    var psDeliveryInfoService =
        (PSDeliveryInfoService) PSDeliveryInfoServiceLocator.getDeliveryInfoService();
    var serverList = psDeliveryInfoService.getAdminUrls(publishServer);
    serverList.add(IPSPubServerService.DEFAULT_DTS);
    return JsonMapper.builder().build().writeValueAsString(serverList.toArray());
  }

  /** Checks if the current instance is an EC2 instance. */
  @GET
  @Path("/isEC2Instance")
  @Produces(MediaType.TEXT_PLAIN)
  public Boolean isEc2Instance() {
    return PSPubServerService.isEC2Instance();
  }

  /** Determines if the default publish server for a site is modified. */
  @GET
  @Path("/isDefaultServerModified/{siteId}")
  @Produces(MediaType.TEXT_PLAIN)
  public Boolean isDefaultServerModified(@PathParam("siteId") String siteId) {
    return service.isDefaultServerModified(siteId);
  }

  /** Gets the default folder location for a publishing server. */
  @GET
  @Path("/defaultFolderLocation/{siteId}/{publishType}/{driver}/{serverType}")
  @Produces(MediaType.TEXT_PLAIN)
  public String getDefaultFolderLocation(
      @PathParam("siteId") String siteId,
      @PathParam("publishType") String publishType,
      @PathParam("driver") String driver,
      @PathParam("serverType") String serverType) {
    try {
      PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "siteId", siteId);
      PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "publishType", publishType);
      PSParameterValidationUtils.rejectIfBlank("defaultFolderLocation", "driver", driver);
      return service.getDefaultFolderLocation(siteId, publishType, driver, serverType);
    } catch (PSValidationException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  /** Gets all available delivery servers. */
  @GET
  @Path("/availableDeliveryServers")
  @Produces(MediaType.TEXT_PLAIN)
  public String getAvailableDeliveryServers() throws tools.jackson.core.JacksonException {
    IPSDeliveryInfoService svc = PSDeliveryInfoServiceLocator.getDeliveryInfoService();
    return JsonMapper.builder().build().writeValueAsString(svc.findAll());
  }
}
