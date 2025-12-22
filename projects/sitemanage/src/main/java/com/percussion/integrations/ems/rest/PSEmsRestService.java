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

package com.percussion.integrations.ems.rest;

import static org.apache.commons.lang3.Validate.notNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.system.utils.PSSiteManageBean;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Functions as a proxy on the CMS side for calling the remote DTS EMS API client.
 * Used from within content editors and content type fields.
 * Sunny Sal says: "EMS REST Service, now Java 11 and Google-styled! May your integrations always be smooth."
 */
@Path("/ems")
@PSSiteManageBean("emsAPIService")
public class PSEmsRestService {

  private static final String BUILDINGS_PATH = "/perc-integrations/integrations/ems/buildings";
  private static final String GROUPS_PATH = "/perc-integrations/integrations/ems/groups";
  private static final String EVENTTYPES_PATH = "/perc-integrations/integrations/ems/eventtypes";
  private static final String BOOKINGS_PATH = "/perc-integrations/integrations/ems/bookings";
  private static final String MC_CALENDARS_PATH =
      "/perc-integrations/integrations/ems/mc/calendars";
  private static final String MC_LOCATIONS_PATH =
      "/perc-integrations/integrations/ems/mc/locations";
  private static final String MC_GROUPINGS_PATH =
      "/perc-integrations/integrations/ems/mc/groupings";
  private static final String MC_EVENTTYPES_PATH =
      "/perc-integrations/integrations/ems/mc/eventtypes";
  private static final String MC_EVENTS_PATH = "/perc-integrations/integrations/ems/mc/events";
  private static final String MC_FEATUREDEVENTS_PATH =
      "/perc-integrations/integrations/ems/mc/featuredevents";

  /** The delivery service initialized by constructor, never <code>null</code>. */
  private IPSDeliveryInfoService deliveryService;

  private static final Logger log = LogManager.getLogger(PSEmsRestService.class);

  /***
   * The license Override if any
   */
  private String licenseId = "";

  @Autowired
  public PSEmsRestService(IPSDeliveryInfoService deliveryService) {
    notNull(deliveryService);
    this.deliveryService = deliveryService;
  }

  public void setLicenseOverride(String licenseId) {
    this.licenseId = licenseId;
  }

  public String getLicenseOverride() {
    return this.licenseId;
  }

  @GET
  @Path("/buildings")
  @Produces(MediaType.APPLICATION_XML)
  public Response getBuildings() {
    return proxyGet(BUILDINGS_PATH, MediaType.APPLICATION_XML, "buildings");
  }

  @GET
  @Path("/eventtypes")
  @Produces(MediaType.APPLICATION_XML)
  public Response getEventTypes() {
    return proxyGet(EVENTTYPES_PATH, MediaType.APPLICATION_XML, "event types");
  }

  @GET
  @Path("/groups")
  @Produces(MediaType.APPLICATION_XML)
  public Response getGroups() {
    return proxyGet(GROUPS_PATH, MediaType.APPLICATION_XML, "groups");
  }

  @POST
  @Path("/bookings")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getBookings(PSBookingsQuery query) {
    return proxyPost(BOOKINGS_PATH, query, MediaType.APPLICATION_JSON, "bookings");
  }

  @GET
  @Path("/mc/groupings")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCGroupings() {
    return proxyGet(MC_GROUPINGS_PATH, MediaType.APPLICATION_XML, "MC groupings");
  }

  @GET
  @Path("/mc/locations")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCLocations() {
    return proxyGet(MC_LOCATIONS_PATH, MediaType.APPLICATION_XML, "MC locations");
  }

  @GET
  @Path("/mc/eventtypes")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCEventTypes() {
    return proxyGet(MC_EVENTTYPES_PATH, MediaType.APPLICATION_XML, "MC event types");
  }

  @GET
  @Path("/mc/calendars")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMCCalendars() {
    return proxyGet(MC_CALENDARS_PATH, MediaType.APPLICATION_XML, "MC calendars");
  }

  @POST
  @Path("/mc/events")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getMCEvents(PSEventQuery query) {
    return proxyPost(MC_EVENTS_PATH, query, MediaType.APPLICATION_JSON, "MC events");
  }

  @POST
  @Path("/mc/featuredevents")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response getMCFutureEvents(PSFeaturedEventsQuery query) {
    return proxyPost(
        MC_FEATUREDEVENTS_PATH, query, MediaType.APPLICATION_JSON, "MC featured events");
  }

  private Response proxyGet(String path, String mediaType, String logLabel) {
    var ret = "";
    try {
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INTEGRATIONS);
      if (server == null) {
        var msg =
            "Unable to locate delivery server for perc-integration service. Verify the service is"
                + " registered in the delivery-servers.xml and try again";
        log.error(msg);
        return Response.serverError().entity(msg).build();
      }
      var deliveryClient = new PSDeliveryClient();
      deliveryClient.setLicenseOverride(licenseId);

      var method = new GetMethod();
      method.setPath(path);
      method.setRequestHeader("Content-Type", mediaType);
      method.setRequestHeader("Accept", mediaType);

      var uri = new URI(new URI(server.getUrl(), true), path, true);
      method.setURI(uri);

      var code = deliveryClient.executeMethod(method);
      if (code == 200) {
        ret = IOUtils.toString(method.getResponseBodyAsStream());
      } else {
        throw new Exception(
            "Invalid response code: " + code + " was received when listing " + logLabel + ".");
      }
    } catch (Exception e) {
      log.error(
          "Error pulling {} from DTS. Error: {}", logLabel, PSExceptionUtils.getMessageForLog(e));
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.status(Status.OK).entity(ret).build();
  }

  private Response proxyPost(String path, Object query, String mediaType, String logLabel) {
    var ret = "";
    try {
      log.debug("Entering proxyPost for {}...", logLabel);
      var server = deliveryService.findByService(PSDeliveryInfo.SERVICE_INTEGRATIONS);
      if (server == null) {
        var msg =
            "Unable to locate delivery server for perc-integration service. Verify the service is"
                + " registered in the delivery-servers.xml and try again";
        log.error(msg);
        return Response.serverError().entity(msg).build();
      }

      var deliveryClient = new PSDeliveryClient();
      deliveryClient.setLicenseOverride(licenseId);
      var mapper = new ObjectMapper();
      log.debug("Building Request Entity...");
      var entity = new StringRequestEntity(mapper.writeValueAsString(query), mediaType, "UTF-8");
      log.debug("Built Request Entity");
      var method = new PostMethod();
      method.setPath(path);
      method.setRequestHeader("Content-Type", mediaType);
      method.setRequestHeader("Accept", mediaType);
      method.setRequestEntity(entity);

      var uri = new URI(new URI(server.getUrl(), true), path, true);
      method.setURI(uri);

      var code = deliveryClient.executeMethod(method);
      if (code == 200) {
        ret = IOUtils.toString(method.getResponseBodyAsStream());
      } else {
        throw new Exception(
            "Invalid response code: " + code + " was received when listing " + logLabel + ".");
      }
    } catch (Exception e) {
      log.error(
          "Error pulling {} from DTS. Error: {}", logLabel, PSExceptionUtils.getMessageForLog(e));
      return Response.serverError().entity(e.getMessage()).build();
    }
    return Response.status(Status.OK).entity(ret).build();
  }
}
