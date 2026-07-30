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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentIds;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.services.IPSRestService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * REST API for comment operations.
 *
 * @author natechadwick
 */
public interface IPSCommentRestService extends IPSRestService {

  /**
   * Returns all comments based on the passed in comments criteria.
   *
   * @param criteria a comments criteria object. See {@link PSCommentCriteria} for a description of
   *     each allowed criteria field. Cannot be null.
   * @return comments object, never null, may be empty.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  PSComments getComments(PSCommentCriteria criteria);

  /**
   * Returns all comments based on the passed in comments criteria. Comments are viewed as
   * moderator. They will be marked as viewed.
   *
   * @param criteria cannot be null.
   * @return comments object, never null, may be empty.
   */
  @POST
  @RolesAllowed("deliverymanager")
  @Path("/moderation/asmoderator")
  @Produces(MediaType.APPLICATION_JSON)
  PSComments getCommentsAsModerator(PSCommentCriteria criteria);

  /**
   * Calls {@link #getComments(PSCommentCriteria)} and wraps for a jsonp call for cross-domain
   * access to JavaScript.
   *
   * @param criteria comment criteria.
   * @return wrapped comments object, never null or empty. But the wrapper object may contain no
   *     comment data.
   */
  @POST
  @Path("/jsonp")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  GenericEntity<PSComments> getCommentsP(PSCommentCriteria criteria);

  /**
   * Retrieves list of page summaries for the specified site.
   *
   * @param site the sitename of the site to find pages with comments for.
   * @param criteria comment criteria for paging.
   * @return list of page summaries, never null, may be empty.
   */
  @POST
  @Path("/pageswithcomments/{site}")
  @Produces(MediaType.APPLICATION_JSON)
  PSPageSummaries getPagesWithComments(@PathParam("site") String site, PSCommentCriteria criteria);

  /**
   * Processes a comments entry form and adds a new comment to the comment service. Upon comment
   * addition the form redirects back to the referer.
   *
   * @param containerRequest request context.
   * @param action form action.
   * @param headers request headers.
   * @return a response that redirects back to the referer after the comment is added.
   */
  @POST
  @Path("/addcomment")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  Response addComment(
      @Context ContainerRequest containerRequest,
      @FormParam("action") String action,
      @Context HttpHeaders headers);

  /**
   * Deletes the specified list of comments.
   *
   * @param commentIds list of comment IDs to be deleted.
   */
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/delete")
  void delete(PSCommentIds commentIds);

  /**
   * Approves the specified list of comments.
   *
   * @param commentIds list of comment IDs to be approved.
   */
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/approve")
  void approve(PSCommentIds commentIds);

  /**
   * Rejects the specified list of comments.
   *
   * @param commentIds list of comment IDs to be rejected.
   */
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/reject")
  void reject(PSCommentIds commentIds);

  /**
   * Sets the default moderation state for a site.
   *
   * @param data map containing site and state.
   */
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/defaultModerationState")
  void setDefaultModerationState(Map<String, Object> data);

  /**
   * Gets the default moderation state for a site.
   *
   * @param site site name.
   * @return moderation state.
   */
  @GET
  @Path("/defaultModerationState/{site}")
  String getDefaultModerationState(@PathParam("site") String site);

  // Form field param constants
  /** Form field name for the comment author's user name. */
  String FORM_PARAM_USERNAME = "username";

  /** Form field name for the site the comment belongs to. */
  String FORM_PARAM_SITE = "site";

  /** Form field name for the relative path of the page being commented on. */
  String FORM_PARAM_PAGEPATH = "pagepath";

  /** Form field name for the comment author's email address. */
  String FORM_PARAM_EMAIL = "email";

  /** Form field name for the comment body text. */
  String FORM_PARAM_TEXT = "text";

  /** Form field name for the comment title. */
  String FORM_PARAM_TITLE = "title";

  /** Form field name for the comma or pipe separated list of comment tags. */
  String FORM_PARAM_TAGS = "tags";

  /** Form field name for a URL supplied by the comment author. */
  String FORM_PARAM_URL = "url";

  // Honeypot spelled backwards.
  /** Honeypot form field name (spelled backwards). Bots fill this in; humans do not. */
  String FORM_PARAM_HONEYPOT = "topyenoh";
}
