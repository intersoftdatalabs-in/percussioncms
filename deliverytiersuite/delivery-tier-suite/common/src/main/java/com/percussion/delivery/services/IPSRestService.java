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
package com.percussion.delivery.services;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Defines an interface to be implemented by all public REST services.
 * This interface defines common methods such as retrieving the current version
 * of a service, intended to be consistent between services.
 * // REFACTORED: CP-JAVA11
 */
public interface IPSRestService {

    /**
     * Returns the currently deployed version of the service.
     *
     * @return the version string
     */
    @GET
    @Path("/version")
    String getVersion();

    /**
     * Fixes behavior in the DTS database after a site is renamed in CM1.
     * Deletes old DTS database information after a site is renamed in CM1.
     * Prior to this fix, old data after the rename was left behind.
     *
     * @param prevSiteName the old name for the site
     * @param newSiteName the new name for the site
     * @return {@code 204} if the process was successful; error code otherwise
     * @see com.percussion.services.siterename.IPSSiteRenameService#deleteDTSEntries
     */
    @DELETE
    @Path("/updateOldSiteEntries/{prevSiteName}/{newSiteName}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("deliverymanager")
    Response updateOldSiteEntries(@PathParam("prevSiteName") String prevSiteName,
                                  @PathParam("newSiteName") String newSiteName);
}
