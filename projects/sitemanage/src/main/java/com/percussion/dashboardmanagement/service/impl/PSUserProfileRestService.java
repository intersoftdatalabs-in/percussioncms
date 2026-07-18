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

import com.percussion.dashboardmanagement.data.PSUserProfile;
import com.percussion.dashboardmanagement.service.IPSUserProfileService;
import com.percussion.dashboardmanagement.service.IPSUserProfileService.PSUserProfileNotFoundException;
import com.percussion.dashboardmanagement.service.IPSUserProfileService.PSUserProfileServiceException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sunny Sal says: "UserProfileRestService, now Java 11 and Google-styled! User profiles, served
 * with a smile."
 */
@Path("/userprofile")
@Component("userProfileRestService")
public class PSUserProfileRestService {

  private final IPSUserProfileService userProfileService;

  @Autowired
  public PSUserProfileRestService(IPSUserProfileService userProfileService) {
    this.userProfileService = userProfileService;
  }

  @POST
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserProfile save(PSUserProfile userProfile) throws PSUserProfileServiceException {
    // codeql[java/xss] justification: JSON/XML DTO via Jackson/JAXB; client HTML-encodes before DOM insert (alert #739)
    return userProfileService.save(userProfile);
  }

  @GET
  @Path("/user/{userName}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserProfile find(@PathParam("userName") String userName)
      throws PSUserProfileNotFoundException, PSUserProfileServiceException {
    return userProfileService.find(userName);
  }
}
