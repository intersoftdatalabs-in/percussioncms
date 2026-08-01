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

/**
 * Implementation of {@link IPSMembershipService}. Sunny Sal says: "Managing members like a pro!"
 */
package com.percussion.membership.service.impl;

import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.PSDeliveryInfoServiceLocator;
import com.percussion.membership.data.PSAccountSummary;
import com.percussion.membership.data.PSUserGroup;
import com.percussion.membership.data.PSUserSummaries;
import com.percussion.membership.data.PSUserSummary;
import com.percussion.membership.service.IPSMembershipService;
import com.percussion.pubserver.IPSPubServerService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.error.PSNotFoundException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implementation of {@link IPSMembershipService}. Sunny Sal says: "Managing members like a pro!"
 */
@Path(IPSMembershipService.MEMBERSHIP)
@Component("membershipService")
@Lazy
public class PSMembershipService implements IPSMembershipService {

  @Autowired @Lazy private IPSPubServerService pubServerService;

  @Override
  @GET
  @Path(ADMIN_USERS + "/{site}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserSummaries getUsers(@PathParam("site") String site) {
    try {
      var adminUrl = pubServerService.getDefaultAdminURL(site);
      var deliveryService = PSDeliveryInfoServiceLocator.getDeliveryInfoService();
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_MEMBERSHIP, null, adminUrl);
      if (server == null) {
        throw new WebApplicationException(
            "Cannot find service of: " + PSDeliveryInfo.SERVICE_MEMBERSHIP);
      }

      var url = "/" + PSDeliveryInfo.SERVICE_MEMBERSHIP + MEMBERSHIP + ADMIN_USERS;
      var summaries = new ArrayList<PSUserSummary>();
      var deliveryClient = new PSDeliveryClient();

      List<Object> users =
          (List<Object>) deliveryClient.getJsonArray(new PSDeliveryActionOptions(server, url));
      for (var i = 0; i < users.size(); i++) {

        Map<String, Object> userSum = (Map<String, Object>) users.get(i);
        var userSummary = new PSUserSummary();
        userSummary.setEmail((String) userSum.get("email"));
        userSummary.setCreatedDate((String) userSum.get("createdDate"));
        userSummary.setStatus((String) userSum.get("status"));
        userSummary.setGroups((String) userSum.get("groups"));
        summaries.add(userSummary);
      }
      return new PSUserSummaries(summaries);
    } catch (Exception e) {
      log.warn("Error getting all users from the membership service: Error: {}", e.getMessage());
      throw new WebApplicationException(e);
    }
  }

  @Override
  @PUT
  @Path(ADMIN_ACCOUNT + "/{site}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserSummaries changeStateAccount(
      PSAccountSummary account, @PathParam("site") String site) {
    try {
      var adminUrl = pubServerService.getDefaultAdminURL(site);
      var deliveryService = PSDeliveryInfoServiceLocator.getDeliveryInfoService();
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_MEMBERSHIP, null, adminUrl);
      if (server == null) {
        throw new WebApplicationException(
            "Cannot find service of: " + PSDeliveryInfo.SERVICE_MEMBERSHIP);
      }

      var url = "/" + PSDeliveryInfo.SERVICE_MEMBERSHIP + MEMBERSHIP + ADMIN_ACCOUNT;
      var deliveryClient = new PSDeliveryClient();
      var accountJson = new java.util.LinkedHashMap<String, Object>();
      accountJson.put("email", account.getEmail().orElse(""));
      accountJson.put("action", account.getAction().orElse(""));
      deliveryClient.push(
          new PSDeliveryActionOptions(server, url, HttpMethodType.PUT, true),
          JsonMapper.builder().build().writeValueAsString(accountJson));

      return getUsers(site);
    } catch (Exception e) {
      log.warn("Error changing membership account type: {}", PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @Override
  @DELETE
  @Path(ADMIN_ACCOUNT + "/{email:.*}/{site}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserSummaries deleteAccount(
      @PathParam("email") String email, @PathParam("site") String site) {
    try {
      var adminUrl = pubServerService.getDefaultAdminURL(site);
      var deliveryService = PSDeliveryInfoServiceLocator.getDeliveryInfoService();
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_MEMBERSHIP, null, adminUrl);
      if (server == null) {
        throw new WebApplicationException(
            "Cannot find service of: " + PSDeliveryInfo.SERVICE_MEMBERSHIP);
      }

      var url = "/" + PSDeliveryInfo.SERVICE_MEMBERSHIP + MEMBERSHIP + ADMIN_ACCOUNT + "/" + email;
      var deliveryClient = new PSDeliveryClient();
      deliveryClient.push(
          new PSDeliveryActionOptions(server, url, HttpMethodType.DELETE, true), "");

      return getUsers(site);
    } catch (IPSPubServerService.PSPubServerServiceException | PSNotFoundException e) {
      log.warn("Error deleting user(s) from the membership service.  Error: {}", e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e);
    }
  }

  @Override
  @PUT
  @Path(ADMIN_USER_GROUP + "/{site}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSUserSummaries updateGroupAccount(PSUserGroup userGroup, @PathParam("site") String site) {
    try {
      var adminUrl = pubServerService.getDefaultAdminURL(site);
      var deliveryService = PSDeliveryInfoServiceLocator.getDeliveryInfoService();
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_MEMBERSHIP, null, adminUrl);
      if (server == null) {
        throw new WebApplicationException(
            "Cannot find service of: " + PSDeliveryInfo.SERVICE_MEMBERSHIP);
      }

      var url =
          "/" + PSDeliveryInfo.SERVICE_MEMBERSHIP + MEMBERSHIP + ADMIN_USER_GROUP + "/" + site;
      var deliveryClient = new PSDeliveryClient();
      var accountJson = new java.util.LinkedHashMap<String, Object>();
      accountJson.put("email", userGroup.getEmail().orElse(""));
      accountJson.put("groups", userGroup.getGroups().orElse(""));
      deliveryClient.push(
          new PSDeliveryActionOptions(server, url, HttpMethodType.PUT, true),
          JsonMapper.builder().build().writeValueAsString(accountJson));

      return getUsers(site);
    } catch (IPSPubServerService.PSPubServerServiceException
        | PSNotFoundException
        | tools.jackson.core.JacksonException e) {
      log.warn("Error updating group account.  Error: {}", e.getMessage());
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /** Logger for this service. */
  public static final Logger log = LogManager.getLogger(PSMembershipService.class);
}
