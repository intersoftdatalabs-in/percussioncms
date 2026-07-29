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
package com.percussion.delivery.feeds.services;

import com.percussion.delivery.feeds.data.PSFeedDTO;
import com.percussion.delivery.feeds.data.PSFeedDescriptors;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.delivery.services.IPSRestService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST service contract for the feeds module. Exposes feed generation, external feed proxying, feed
 * descriptor persistence, and metadata listener management.
 *
 * @author natechadwick
 */
public interface IPSFeedsRestService extends IPSRestService {

  /**
   * Retrieve the feed descriptor for the specified feed and generate the feed from data in the
   * dynamic indexing service.
   *
   * @param sitename the site the feed belongs too, may be <code>null</code> or empty in which case
   *     a page not found will be sent in response.
   * @param feedname may be <code>null</code> or empty in which case a page not found will be sent
   *     in response.
   * @param hostname the host name used to build the feed's links, never <code>null</code>
   * @param httpRequest the current HTTP servlet request, never <code>null</code>
   * @return response with the feed xml or a page not found or server error, depending on the
   *     situation.
   */
  @GET
  @Path("/{sitename}/{feedname}/{hostname}")
  @Produces("text/xml")
  public abstract Response getFeed(
      @PathParam("sitename") String sitename,
      @PathParam("feedname") String feedname,
      @PathParam("hostname") String hostname,
      @Context HttpServletRequest httpRequest);

  /**
   * Acts as a proxy getting a list of feeds from an external URL. Returns the xml as a string.
   *
   * @param psFeedDTO the feed DTO containing the target URL, assumed to not be <code>null</code>.
   * @return the external feed XML as a string, never <code>null</code>
   */
  @POST
  @Path("/readExternalFeed")
  @Produces(MediaType.APPLICATION_XML)
  public abstract String readExternalFeed(PSFeedDTO psFeedDTO);

  /**
   * Saves the feed descriptors and connection info for meta data service. It is expected that all
   * public descriptors are sent at once by the CM1 server. A difference will be done between the
   * list sent and currently stored descriptors. Any stored descriptors not on the list sent will be
   * deleted. Notifies listeners of changes so that cache regions can be flushed.
   *
   * @param descriptors the feed descriptors and connection info to save, never <code>null</code>
   */
  @PUT
  @Path("/descriptors")
  @RolesAllowed("deliverymanager")
  public abstract void saveDescriptors(PSFeedDescriptors descriptors);

  /**
   * Adds a metadata change listener.
   *
   * @param listener the listener to register, never <code>null</code>
   */
  public abstract void addMetadataListener(IPSServiceDataChangeListener listener);

  /**
   * Removes a previously registered metadata change listener.
   *
   * @param listener the listener to remove, never <code>null</code>
   */
  public abstract void removeMetadataListener(IPSServiceDataChangeListener listener);

  /**
   * Rotates the encryption key used to protect stored credentials.
   *
   * @param key the new key supplied by the caller, never <code>null</code>
   */
  @PUT
  @Path("/rotateKey")
  @RolesAllowed("deliverymanager")
  @Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
  public abstract void rotateKey(String key);

  // Property key constants
  /** Property key for the feed item description (Dublin Core: abstract). */
  public static final String PROP_DESCRIPTION = "dcterms:abstract";

  /** Property key for the feed item title (Dublin Core: title). */
  public static final String PROP_TITLE = "dcterms:title";

  /** Property key for the feed item publish date (Dublin Core: created). */
  public static final String PROP_PUBDATE = "dcterms:created";

  /** Property key for the feed item content post date timezone. */
  public static final String PROP_CONTENTPOSTDATETZ = "dcterms:contentpostdatetz";
}
