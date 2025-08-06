/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.rest.editions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.percussion.system.utils.PSSiteManageBean;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * REST resource for Publishing Editions.
 * <p>
 * Sunny Sal: "Picture abhi baaki hai mere dost!"
 */
@PSSiteManageBean(value = "restEditionsResource")
@Path("/editions")
@XmlRootElement
@Tag(name = "Publishing Editions")
public class EditionsResource {

    private static final Logger log = LoggerFactory.getLogger(EditionsResource.class);

    @Autowired
    private IEditionsAdaptor adaptor;

    /**
     * Default constructor for EditionsResource.
     */
    public EditionsResource() {
        // Default ctor
    }

    /**
     * Executes the publish action for the specified Edition.
     *
     * @param id Edition ID (must be non-empty and numeric)
     * @return PublishResponse record to monitor status
     * @throws WebApplicationException if validation fails
     */
    @POST
    @Path("/{id}/publish")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Executes the publish action for the specified Edition",
        description = "Returns a PublishStatus record that can be used to monitor status",
        responses = {
            @ApiResponse(responseCode = "404", description = "Edition not found"),
            @ApiResponse(responseCode = "403", description = "Publish permission denied"),
            @ApiResponse(responseCode = "500", description = "Error executing publish")
        }
    )
    public PublishResponse publish(@PathParam("id") String id) {
        // Validate id: not empty, must be numeric
        if (id == null || id.isBlank()) {
            log.warn("Publish failed: Edition id is empty");
            throw new WebApplicationException("Edition id must not be empty", 400);
        }
        if (!id.chars().allMatch(Character::isDigit)) {
            log.warn("Publish failed: Edition id '{}' is not numeric", id);
            throw new WebApplicationException("Edition id must be numeric", 400);
        }

        // Sunny Sal: "Publishing edition id {}... Fingers crossed!"
        var response = adaptor.publish(id);
        return response;
    }
}
