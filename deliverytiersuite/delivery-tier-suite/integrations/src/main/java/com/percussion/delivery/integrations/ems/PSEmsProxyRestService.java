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
package com.percussion.delivery.integrations.ems;

import com.percussion.delivery.integrations.ems.model.*;
import com.percussion.delivery.utils.lookup.PSLookup;
import com.percussion.delivery.utils.lookup.PSXEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides a lightweight REST proxy for the EMS SOAP API.
 * <p>
 * Exposes endpoints for EMS and Master Calendar lookups and queries.
 * </p>
 */
@Path("/integrations/ems")
@Component
public class PSEmsProxyRestService {
    @Autowired
    private EMSSOAPEventService service;
    @Autowired
    private EMSMasterCalendarSoapEventService mcService;

    public EMSMasterCalendarSoapEventService getMcService() {
        return mcService;
    }
    @Autowired
    public void setMcService(EMSMasterCalendarSoapEventService mcService) {
        this.mcService = mcService;
    }
    public EMSSOAPEventService getService() {
        return service;
    }
    @Autowired
    public void setService(EMSSOAPEventService service) {
        this.service = service;
    }

    @GET
    @Path("/groups")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getGroups() {
        var groups = service.getGroupTypes();
        var lookup = new PSLookup();
        groups.stream()
                .filter(GroupType::isAvailableOnWeb)
                .map(g -> new PSXEntry(String.valueOf(g.getId()), g.getDescription()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/buildings")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getBuildings() {
        var buildings = service.getBuildings();
        var lookup = new PSLookup();
        buildings.stream()
                .map(b -> new PSXEntry(String.valueOf(b.getId()), b.getDescription()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/eventtypes")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getEventTypes() {
        var events = service.getEventTypes();
        var lookup = new PSLookup();
        events.stream()
                .filter(EventType::isDisplayOnWeb)
                .map(e -> new PSXEntry(String.valueOf(e.getId()), e.getDescription()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/statuses")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getStatuses() {
        var statuses = service.getStatus();
        var lookup = new PSLookup();
        statuses.stream()
                .filter(Status::isDisplayOnWeb)
                .map(e -> new PSXEntry(String.valueOf(e.getId()), e.getDescription()))
                .forEach(lookup::add);
        return lookup;
    }

    @POST
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<Booking> getBookings(PSBookingsQuery query) {
        return service.getBookings(query);
    }

    public PSEmsProxyRestService() {}

    @POST
    @Path("/mc/featuredevents")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<MCEventDetail> getFeaturedEvents(PSFeaturedEventsQuery query) {
        return mcService.getMasterCalendarFeaturedEvents(query);
    }

    @POST
    @Path("/mc/events")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<MCEventDetail> getEvents(PSEventQuery query) {
        return mcService.getMasterCalendarEvents(query);
    }

    @GET
    @Path("/mc/locations")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getMCLocations() {
        var lookup = new PSLookup();
        mcService.getMasterCalendarLocations().stream()
                .map(t -> new PSXEntry(String.valueOf(t.getLocationId()), t.getLocationName()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/mc/groupings")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getMCGroupings() {
        var lookup = new PSLookup();
        mcService.getMasterCalendarGroupings().stream()
                .map(t -> new PSXEntry(String.valueOf(t.getGroupingId()), t.getName()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/mc/eventtypes")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getMCEventTypes() {
        var lookup = new PSLookup();
        mcService.getMasterCalendarEventTypes().stream()
                .map(t -> new PSXEntry(String.valueOf(t.getEventTypeId()), t.getEventTypeLocationName()))
                .forEach(lookup::add);
        return lookup;
    }

    @GET
    @Path("/mc/calendars")
    @Produces(MediaType.APPLICATION_XML)
    public PSLookup getMCCalendars() {
        var lookup = new PSLookup();
        mcService.getMasterCalendarCalendars().stream()
                .filter(c -> !c.getPrivateCalendar() && c.getActiveCalendar())
                .map(c -> new PSXEntry(String.valueOf(c.getCalendarId()), c.getCalendarName()))
                .forEach(lookup::add);
        return lookup;
    }
}
