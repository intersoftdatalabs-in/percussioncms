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


/**
 * @author natechadwick
 *
 */
public interface IPSCommentRestService extends IPSRestService {

	/**
	 * Returns all comments based on the passed in comments criteria.
	 * 
	 * @url /perc-comments-services/comment
	 * @httpverb POST
	 * @nullipotent yes (read-only method).
	 * @secured no.
	 * @param criteria a comments criteria object. See {@link PSCommentCriteria} for
	 * a description of each allowed criteria field. Cannot be <code>null</code>.
	 * @return comments object, never <code>null</code> may be empty.
	 * @httpcodeonsuccess HTTP 200.
	 * @httpcodeonerror HTTP 500.
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public abstract PSComments getComments(PSCommentCriteria criteria);

	/**
	 * Returns all comments based on the passed in comments criteria. Comments
	 * are viewed as moderator. They will be marked as viewed.
	 * 
	 * @url /perc-comments-services/comment/moderation/asmoderator
	 * @httpverb POST
	 * @secured yes
	 * @param criteria cannot be <code>null</code>.
	 * @return comments object, never <code>null</code> may be empty.
	 */
	@POST
	@RolesAllowed("deliverymanager")
	@Path("/moderation/asmoderator")
	@Produces("application/json")
	public abstract PSComments getCommentsAsModerator(PSCommentCriteria criteria);

	/**
	 * Calls {@link #getComments(PSCommentCriteria)} and wraps for a jsonp call
	 * for cross-domain access to javascript.
	 * @param callback the callback name to be used for jsonp wrapping, defaults to
	 * <code>fn</code>.
	 * @param pagepath the page path not including site. This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param site See {@link PSCommentCriteria#getSite()}.
	 * <p>
	 * This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param username This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param state This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param maxResults This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param startIndex This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param tag This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param sortby This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param ascending This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param moderated This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @param viewed This is a querystring param
	 * and is not required and may be <code>null</code> or empty.
	 * @return wrapped comments object, never <code>null</code> or empty. But the wrapper object
	 * may contain no comment data.
	 */
	@POST
	@Path("/jsonp")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	//@JSONP(callback = "fn", queryParam = "callback")

	public abstract GenericEntity getCommentsP(PSCommentCriteria criteria/*
			@QueryParam("callback") @DefaultValue("fn") String callback,
			@QueryParam("pagepath") String pagepath,
			@QueryParam("site") String site,
			@QueryParam("username") String username,
			@QueryParam("state") String state,
			@QueryParam("maxresults") String maxResults,
			@QueryParam("startindex") String startIndex,
			@QueryParam("tag") String tag, @QueryParam("sortby") String sortby,
			@QueryParam("ascending") String ascending,
			@QueryParam("moderated") String moderated,
			@QueryParam("viewed") String viewed,
			@QueryParam("lastcommentid") String lastCommentId*/);

	/**
	 * Retrieves list of page summaries for the specified site.  Page summaries
	 * include the pagepath, total count, and approved count.
	 * @param site the sitename of the site to find pages with comments for.
	 * @param maxResults the max results that should be returned, may be <code>null</code>
	 * or empty. Used for paging.
	 * @param startIndex the stating index of the page result to send back, may
	 * be <code>null</code>
	 * or empty. Used for paging.
	 * @return list of page summaries, never <code>null</code>, may be empty.
	 */
	@POST
	@Path("/pageswithcomments/{site}")
	@Produces("application/json")
	public abstract PSPageSummaries getPagesWithComments(
			@PathParam("site") String site,
            PSCommentCriteria criteria);

	/**
	 * Processes a comments entry form and adds a new comment to the comment
	 * service. Upon comment addition the form redirects back to the referer.
	 * @param params the multivaluedmap of all parameters passed in by the form.
	 * Never <code>null</code>, may be empty.
	 * @param headers the multivaluedmap of request header parameters. Never
	 * <code>null</code>, may be empty.
	 */
	@POST
	@Path("/addcomment")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public abstract Response addComment( @Context ContainerRequest containerRequest,
										@FormParam("action") String action, @Context HttpHeaders headers);

	/**
	 * Deletes the specified list of comments.
	 * 
	 * @param commentIds An object which represent a list of comment IDs
	 * to be deleted.
	 */
	@PUT
	@RolesAllowed("deliverymanager")
	@Path("/moderation/delete")
	public abstract void delete(PSCommentIds commentIds);

	/**
	 * Approves the specified list of comments.
	 * 
	 * @param commentIds An object which represent a list of comment IDs
	 * to be deleted.
	 */
	@PUT
	@RolesAllowed("deliverymanager")
	@Path("/moderation/approve")
	public abstract void approve(PSCommentIds commentIds);

	/**
	 * Rejects the specified list of comments.
	 * 
	 * @param commentIds An object which represent a list of comment IDs
	 * to be deleted.
	 */
	@PUT
	@RolesAllowed("deliverymanager")
	@Path("/moderation/reject")
	public abstract void reject(PSCommentIds commentIds);

	/**
	 * 
	 * @param data
	 */
	@PUT
	@RolesAllowed("deliverymanager")
	@Path("/moderation/defaultModerationState")
	public abstract void setDefaultModerationState(Map data);

	/**
	 * 
	 * @param site
	 * @return
	 */
	@GET
	@Path("/defaultModerationState/{site}")
	public abstract String getDefaultModerationState(
			@PathParam("site") String site);

	/* Form field param constants */
	public static final String FORM_PARAM_USERNAME = "username";
	public static final String FORM_PARAM_SITE = "site";
	public static final String FORM_PARAM_PAGEPATH = "pagepath";
	public static final String FORM_PARAM_EMAIL = "email";
	public static final String FORM_PARAM_TEXT = "text";
	public static final String FORM_PARAM_TITLE = "title";
	public static final String FORM_PARAM_TAGS = "tags";
	public static final String FORM_PARAM_URL = "url";
	// honeypot spelled backwards.  in case bots are looking for "honeypot"
	public static final String FORM_PARAM_HONEYPOT = "topyenoh";

}
