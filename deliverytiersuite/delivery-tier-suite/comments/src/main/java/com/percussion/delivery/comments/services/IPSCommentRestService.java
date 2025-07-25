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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentIds;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.services.IPSRestService;
import org.glassfish.jersey.server.ContainerRequest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;


/**
 * REST interface for comment operations in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public interface IPSCommentRestService extends IPSRestService {

    /**
     * Returns all comments based on the passed in comments criteria.
     *
     * @param criteria a comments criteria object. See {@link PSCommentCriteria} for
     *                 a description of each allowed criteria field. Cannot be null.
     * @return comments object, never null; may be empty.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    PSComments getComments(PSCommentCriteria criteria);

    /**
     * Returns all comments as moderator. Comments will be marked as viewed.
     *
     * @param criteria cannot be null.
     * @return comments object, never null; may be empty.
     */
    @POST
    @RolesAllowed("deliverymanager")
    @Path("/moderation/asmoderator")
    @Produces(MediaType.APPLICATION_JSON)
    PSComments getCommentsAsModerator(PSCommentCriteria criteria);

    /**
     * Wraps comments for a JSONP call for cross-domain access to JavaScript.
     *
     * @param criteria comment criteria
     * @return wrapped comments object, never null; may be empty.
     */
    @POST
    @Path("/jsonp")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    GenericEntity getCommentsP(PSCommentCriteria criteria);

    /**
     * Retrieves list of page summaries for the specified site.
     *
     * @param site the site name
     * @param criteria comment criteria
     * @return list of page summaries, never null; may be empty.
     */
    @POST
    @Path("/pageswithcomments/{site}")
    @Produces(MediaType.APPLICATION_JSON)
    PSPageSummaries getPagesWithComments(@PathParam("site") String site, PSCommentCriteria criteria);

    /**
     * Processes a comments entry form and adds a new comment.
     *
     * @param containerRequest the request context
     * @param action form action
     * @param headers HTTP headers
     * @return HTTP response
     */
    @POST
    @Path("/addcomment")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response addComment(@Context ContainerRequest containerRequest,
                        @FormParam("action") String action,
                        @Context HttpHeaders headers);

    /**
     * Deletes the specified list of comments.
     *
     * @param commentIds list of comment IDs to delete
     */
    @PUT
    @RolesAllowed("deliverymanager")
    @Path("/moderation/delete")
    void delete(PSCommentIds commentIds);

    /**
     * Approves the specified list of comments.
     *
     * @param commentIds list of comment IDs to approve
     */
    @PUT
    @RolesAllowed("deliverymanager")
    @Path("/moderation/approve")
    void approve(PSCommentIds commentIds);

    /**
     * Rejects the specified list of comments.
     *
     * @param commentIds list of comment IDs to reject
     */
    @PUT
    @RolesAllowed("deliverymanager")
    @Path("/moderation/reject")
    void reject(PSCommentIds commentIds);

    /**
     * Sets the default moderation state for a site.
     *
     * @param data moderation state data
     */
    @PUT
    @RolesAllowed("deliverymanager")
    @Path("/moderation/defaultModerationState")
    void setDefaultModerationState(Map data);

    /**
     * Gets the default moderation state for a site.
     *
     * @param site site name
     * @return moderation state
     */
    @GET
    @Path("/defaultModerationState/{site}")
    String getDefaultModerationState(@PathParam("site") String site);

    // Form field param constants
    String FORM_PARAM_USERNAME = "username";
    String FORM_PARAM_SITE = "site";
    String FORM_PARAM_PAGEPATH = "pagepath";
    String FORM_PARAM_EMAIL = "email";
    String FORM_PARAM_TEXT = "text";
    String FORM_PARAM_TITLE = "title";
    String FORM_PARAM_TAGS = "tags";
    String FORM_PARAM_URL = "url";
    // honeypot spelled backwards
    String FORM_PARAM_HONEYPOT = "topyenoh";
}
