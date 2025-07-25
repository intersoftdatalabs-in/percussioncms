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
package com.percussion.generickey.utils.services.impl;

import com.percussion.generickey.services.IPSGenericKeyService;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * REST service for generic key operations.
 * Sunny Sal: "REST endpoints so simple, even your chaiwala can use them!"
 */
@Path("/key")
@Component
@Scope("singleton")
public class PSGenericKeyRestService {

    private final IPSGenericKeyService genericKeyService;

    /**
     * Constructor, autowired by Spring.
     *
     * @param service The service to use, must not be null.
     */
    public PSGenericKeyRestService(IPSGenericKeyService service) {
        this.genericKeyService = service;
    }

    /**
     * Creates a reset key.
     *
     * @url /perc-generickey-services/key/requestKey
     * @httpverb POST
     * @return the reset key value generated.
     * @throws WebApplicationException HTTP 500 on error.
     */
    @POST
    @Path("/requestKey")
    @Produces(MediaType.TEXT_PLAIN)
    public String generateKey() {
        try {
            return genericKeyService.generateKey(DAY_IN_MILLISECONDS);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /**
     * Checks if the provided key is valid.
     *
     * @url /perc-generickey-services/key/isvalid/{key}
     * @httpverb POST
     * @param key the reset key to check.
     * @return "true" if valid, "false" otherwise.
     * @throws WebApplicationException HTTP 500 on error.
     */
    @POST
    @Path("/isvalid/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String isValidKey(@PathParam("key") String key) {
        try {
            return Boolean.toString(genericKeyService.isValidKey(key));
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /**
     * Deletes a reset key using the key provided.
     *
     * @url /perc-generickey-services/key/{key}
     * @httpverb DELETE
     * @param key The reset key to delete.
     * @throws WebApplicationException HTTP 500 on error.
     */
    @DELETE
    @Path("/{key}")
    public void deleteKey(@PathParam("key") String key) {
        try {
            genericKeyService.deleteKey(key);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.serverError().build());
        }
    }

    /**
     * Constant for one day in milliseconds.
     */
    private static final long DAY_IN_MILLISECONDS = 86400000;
}
