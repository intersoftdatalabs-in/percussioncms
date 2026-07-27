// REFACTORED: CP-JAVA11
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
package com.percussion.delivery.likes.services;

import com.percussion.delivery.services.PSAbstractRestService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST/Webservice layer used to access the likes service.
 *
 * @author davidpardini
 */
@Path("/likes")
@Component
public class PSLikesRestService extends PSAbstractRestService {

  /** The likes service reference. Initialized in the ctor. Never <code>null</code>. */
  private IPSLikesService likesService;

  private static final Logger log = LogManager.getLogger(PSLikesRestService.class);

  /**
   * Creates a new likes REST service backed by the supplied likes service.
   *
   * @param service the underlying likes service, must not be {@code null}.
   */
  @Inject
  @Autowired
  public PSLikesRestService(IPSLikesService service) {
    likesService = service;
  }

  /**
   * Tally of how many users have Liked a page, a comment.
   *
   * @param site the site the like is associated with, taken from the URL path. Must not be {@code
   *     null} or empty.
   * @param type the type of the liked entity, taken from the URL path. Must not be {@code null}.
   * @param likeId the id of the liked entity, taken from the URL path. Must not be {@code null} or
   *     empty.
   * @return int, never <code>null</code> may be empty.
   */
  @POST
  @Path("/total/{site}/{type}/{likeId:.*}")
  @Produces("application/json")
  public int getTotalLikes(
      @PathParam("site") String site,
      @PathParam("type") String type,
      @PathParam("likeId") String likeId) {
    try {
      return likesService.getTotalLikes(site, likeId, type);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /**
   * To Like a page, a comment.
   *
   * @param site the site the like is associated with, taken from the URL path. Must not be {@code
   *     null} or empty.
   * @param type the type of the liked entity, taken from the URL path. Must not be {@code null}.
   * @param likeId the id of the liked entity, taken from the URL path. Must not be {@code null} or
   *     empty.
   * @return int, never <code>null</code> may be empty.
   */
  @POST
  @Path("/like/{site}/{type}/{likeId:.*}")
  @Produces("application/json")
  public int like(
      @PathParam("site") String site,
      @PathParam("type") String type,
      @PathParam("likeId") String likeId) {
    try {
      return likesService.like(site, likeId, type);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /**
   * To UnLike a page, a comment.
   *
   * @param site the site the like is associated with, taken from the URL path. Must not be {@code
   *     null} or empty.
   * @param type the type of the liked entity, taken from the URL path. Must not be {@code null}.
   * @param likeId the id of the liked entity, taken from the URL path. Must not be {@code null} or
   *     empty.
   * @return int, never <code>null</code> may be empty.
   */
  @POST
  @Path("/unlike/{site}/{type}/{likeId:.*}")
  @Produces("application/json")
  public int unlike(
      @PathParam("site") String site,
      @PathParam("type") String type,
      @PathParam("likeId") String likeId) {
    try {
      return likesService.unlike(site, likeId, type);
    } catch (Exception e) {
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  @Override
  public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
    log.info("Attempting to update likes service with site name: {}", prevSiteName);
    likesService.updateLikesForSiteAfterRename(prevSiteName, newSiteName);
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
