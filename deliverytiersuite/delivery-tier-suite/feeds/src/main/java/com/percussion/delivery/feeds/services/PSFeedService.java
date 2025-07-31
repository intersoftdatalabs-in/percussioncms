// REFACTORED: CP-JAVA11

/*
 * Copyright 1999-2023 Percussion Software, Inc.
 * All rights reserved.
 */
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.feeds.PSFeedGenerator;
import com.percussion.delivery.feeds.data.FeedType;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedDTO;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.percussion.delivery.http.PSHttpClient;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST service for feed generation and management.
 */
@Path("/feeds")
@Service
public class PSFeedService {
    private static final Logger log = LogManager.getLogger(PSFeedService.class);

    private final PSFeedGenerator feedGenerator;
    private final IPSFeedDao feedDao;
    private final PSHttpClient httpClient;

    @Inject
    public PSFeedService(PSFeedGenerator feedGenerator, IPSFeedDao feedDao, PSHttpClient httpClient) {
        this.feedGenerator = Objects.requireNonNull(feedGenerator, "FeedGenerator must not be null");
        this.feedDao = Objects.requireNonNull(feedDao, "FeedDao must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "HttpClient must not be null");
    }

    @GET
    @Path("/{site}/{name}")
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML})
    @RolesAllowed({"cm-user", "everyone"})
    public Response getFeed(@PathParam("site") String site,
                          @PathParam("name") String name,
                          @Context HttpServletRequest request) {
        try {
            validateSiteAndName(site, name);
            String host = extractHost(request);
            List<PSFeedItem> items = getFeedItems(site, name);
            IPSFeedDescriptor descriptor = getFeedDescriptor(site, name)
                .orElseThrow(() -> new NotFoundException("Feed not found: " + site + "/" + name));

            String content = feedGenerator.makeFeedContent(descriptor, host, items);
            return Response.ok(content)
                         .type(getMediaType(descriptor))
                         .build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate feed {}/{}: {}", site, name, e.getMessage());
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateSiteAndName(String site, String name) {
        Objects.requireNonNull(site, "Site must not be null");
        Objects.requireNonNull(name, "Feed name must not be null");
        Validate.matchesPattern(site, "^[a-zA-Z0-9_-]+$", "Invalid site name");
        Validate.matchesPattern(name, "^[a-zA-Z0-9_-]+$", "Invalid feed name");
    }

    private String extractHost(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                      .orElseGet(request::getServerName);
    }

    private Optional<IPSFeedDescriptor> getFeedDescriptor(String site, String name) {
        return feedDao.findByName(name)
                     .filter(descriptor -> site.equals(descriptor.getSite().orElse(null)));
    }

    private List<PSFeedItem> getFeedItems(String site, String name) {
        return getFeedDescriptor(site, name)
                .flatMap(IPSFeedDescriptor::getQuery)
                .map(httpClient::fetchItems)
                .orElse(Collections.emptyList());
    }

    private MediaType getMediaType(IPSFeedDescriptor descriptor) {
        return descriptor.getType()
            .flatMap(FeedType::fromName)
            .map(type -> MediaType.valueOf(type.getMediaType()))
            .orElseThrow(() -> new WebApplicationException(
                "Unsupported feed type: " + descriptor.getType().orElse("unknown"),
                Status.BAD_REQUEST));
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("cm-user")
    public Response getAllFeeds(@Context HttpServletRequest request) {
        try {
            Map<String, List<PSFeedDTO>> feeds = feedDao.findAll().stream()
                .collect(Collectors.groupingBy(
                    descriptor -> descriptor.getSite().orElse("unknown"),
                    Collectors.mapping(
                        descriptor -> new PSFeedDTO(descriptor, extractBaseUrl(request)),
                        Collectors.toUnmodifiableList()
                    )
                ));
            return Response.ok(feeds).build();
        } catch (Exception e) {
            log.error("Failed to retrieve feeds: {}", e.getMessage(), e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractBaseUrl(HttpServletRequest request) {
        return new StringBuilder()
            .append(request.getScheme())
            .append("://")
            .append(request.getServerName())
            .append(":")
            .append(request.getServerPort())
            .append(request.getContextPath())
            .toString();
    }

    @POST
    @Path("/{site}/{name}/refresh")
    @RolesAllowed("cm-user")
    public Response refreshFeed(@PathParam("site") String site,
                              @PathParam("name") String name) {
        try {
            validateSiteAndName(site, name);
            IPSFeedDescriptor descriptor = getFeedDescriptor(site, name)
                .orElseThrow(() -> new NotFoundException("Feed not found: " + site + "/" + name));

            feedGenerator.invalidateCache(descriptor);
            return Response.noContent().build();
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh feed {}/{}: {}", site, name, e.getMessage(), e);
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }
}
