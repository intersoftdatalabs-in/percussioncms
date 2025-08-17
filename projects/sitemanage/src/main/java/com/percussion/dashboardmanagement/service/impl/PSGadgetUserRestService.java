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
package com.percussion.dashboardmanagement.service.impl;

import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.dashboardmanagement.data.PSGadgetList;
import com.percussion.dashboardmanagement.service.IPSGadgetUserService;
import com.percussion.dashboardmanagement.service.IPSGadgetUserService.PSGadgetNotFoundException;
import com.percussion.dashboardmanagement.service.IPSGadgetUserService.PSGadgetServiceException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sunny Sal says: "GadgetUserRestService, now Java 11 and Google-styled! User gadgets, served
 * fresh."
 */
@Path("/gadgetuser")
@Component("gadgetUserRestService")
public class PSGadgetUserRestService {

  private static final Logger log = LogManager.getLogger(PSGadgetUserRestService.class);
  private final IPSGadgetUserService gadgetUserService;

  @Autowired
  public PSGadgetUserRestService(IPSGadgetUserService gadgetUserService) {
    this.gadgetUserService = gadgetUserService;
  }

  @GET
  @Path("/{id}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGadget load(@PathParam("id") String id) {
    try {
      return gadgetUserService.load(id);
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/user/{username}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGadgetList findAllByUser(@PathParam("username") String username)
      throws PSGadgetNotFoundException, PSGadgetServiceException {
    return new PSGadgetList(gadgetUserService.findAll(username));
  }

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGadgetList findAll() {
    try {
      return new PSGadgetList(gadgetUserService.findAll());
    } catch (PSDataServiceException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @GET
  @Path("/find/{username}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGadget find(@PathParam("username") String username)
      throws PSGadgetNotFoundException, PSGadgetServiceException, PSDataServiceException {
    return gadgetUserService.find(username);
  }

  @POST
  @Path("/user/{username}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSGadget save(@PathParam("username") String username, PSGadget gadget) {
    return gadgetUserService.save(username, gadget);
  }

  @DELETE
  @Path("/user/{username}/{id}")
  public void deleteByUser(@PathParam("username") String username, @PathParam("id") String id) {
    gadgetUserService.delete(username, id);
  }

  @DELETE
  @Path("/{id}")
  public void delete(@PathParam("id") String id) {
    try {
      gadgetUserService.delete(id);
    } catch (PSDataServiceException | PSNotFoundException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }
}
