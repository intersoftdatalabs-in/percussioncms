/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.feeds.data.PSFeedDTO;
import com.percussion.delivery.feeds.data.PSFeedDescriptors;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.delivery.services.IPSRestService;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * REST API contract for feed management and generation.
 * Provides endpoints for retrieving, creating, and managing RSS/ATOM feeds.
 */
@Path("/rss")
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
public interface IPSFeedsRestService extends IPSRestService {

    /**
     * Generates a feed for the specified site and name.
     *
     * @param site Site identifier, must match [a-zA-Z0-9_-]+
     * @param name Feed name, must match [a-zA-Z0-9_-]+
     * @param request HTTP request context
     * @return Feed content in RSS/ATOM format
     */
    @GET
    @Path("/{site}/{name}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
    @RolesAllowed({"cm-user", "everyone"})
    Response getFeed(
        @PathParam("site") @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") String site,
        @PathParam("name") @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") String name,
        @Context HttpServletRequest request
    );

    /**
     * Updates feed configuration.
     *
     * @param feedDto Feed configuration
     * @return Updated configuration
     */
    @PUT
    @Path("/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("feed-admin")
    Response updateConfig(@Valid PSFeedDTO feedDto);

    /**
     * Creates new feed descriptors.
     *
     * @param descriptors Feed descriptors to create
     * @return Created descriptors
     */
    @POST
    @Path("/descriptors")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("feed-admin")
    Response createDescriptors(@Valid PSFeedDescriptors descriptors);

    /**
     * Lists all feeds for a site.
     *
     * @param site Site identifier
     * @return List of feed descriptors
     */
    @GET
    @Path("/{site}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"cm-user", "everyone"})
    Response listFeeds(
        @PathParam("site") @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]+") String site
    );

    /**
     * Registers a data change listener.
     *
     * @param listener Service data change listener
     */
    void addServiceDataChangeListener(IPSServiceDataChangeListener listener);

    /**
     * Removes a data change listener.
     *
     * @param listener Service data change listener to remove
     */
    void removeServiceDataChangeListener(IPSServiceDataChangeListener listener);
}
