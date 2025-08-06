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
package com.percussion.integritymanagement.service.rest.impl;

import com.percussion.error.PSExceptionUtils;
import com.percussion.integritymanagement.data.PSIntegrityStatus;
import com.percussion.integritymanagement.data.PSIntegrityStatus.Status;
import com.percussion.integritymanagement.data.PSIntegrityStatusList;
import com.percussion.integritymanagement.service.IPSIntegrityCheckerService.IntegrityTaskType;
import com.percussion.integritymanagement.service.impl.PSIntegrityCheckerService;
import com.percussion.share.service.exception.PSDataServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * REST service for integrity checker operations.
 */
@Service("pSIntegrityCheckerRestService")
@Path("/integritycheck")
public class PSIntegrityCheckerRestService {

    private static final Logger log = LogManager.getLogger(PSIntegrityCheckerRestService.class);

    private final PSIntegrityCheckerService integrityCheckerService;

    @Autowired
    public PSIntegrityCheckerRestService(PSIntegrityCheckerService integrityCheckerService) {
        this.integrityCheckerService = integrityCheckerService;
    }

    @POST
    @Produces({MediaType.TEXT_HTML})
    public String start(@QueryParam("type") String type) {
        try {
            var taskType = IntegrityTaskType.all;
            try {
                taskType = IntegrityTaskType.valueOf(StringUtils.defaultString(type));
            } catch (Exception e) {
                // default to all
            }
            return integrityCheckerService.start(taskType);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/stop")
    public void stop() {
        try {
            integrityCheckerService.stop();
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/token/{id}")
    public PSIntegrityStatus status(@PathParam("id") String id) {
        try {
            return integrityCheckerService.getStatus(id);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/history")
    public List<PSIntegrityStatus> history(@QueryParam("type") String type) {
        try {
            Status st = null;
            try {
                st = Status.valueOf(StringUtils.defaultString(type));
            } catch (Exception e) {
                // default to null
            }
            return new PSIntegrityStatusList(integrityCheckerService.getHistory(st));
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }

    @DELETE
    @Path("/token/{id}")
    public void delete(@PathParam("id") String id) {
        try {
            integrityCheckerService.delete(id);
        } catch (PSDataServiceException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw new WebApplicationException(e);
        }
    }
}
