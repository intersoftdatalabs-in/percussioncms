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

import com.percussion.delivery.comments.data.*;
import com.percussion.delivery.exceptions.PSBadRequestException;
import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.error.PSExceptionUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST service for managing comments in the CMS.
 * Thread-safe and uses modern Java 11 features.
 */
@Path("/comment")
@Component
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class PSCommentsRestService extends PSAbstractRestService implements IPSCommentRestService {
    private static final Logger log = LogManager.getLogger(PSCommentsRestService.class);
    private static final String CALLBACK_PARAM = "callback";
    private static final DateTimeFormatter ISO8601_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
            .withZone(ZoneId.systemDefault());

    private final IPSCommentsService commentService;

    @Inject
    @Autowired
    public PSCommentsRestService(IPSCommentsService service) {
        this.commentService = Objects.requireNonNull(service, "service must not be null");
    }

    @HEAD
    @Path("/csrf")
    public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        var cookies = Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]);
        for (Cookie cookie : cookies) {
            if ("XSRF-TOKEN".equals(cookie.getName())) {
                response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
                response.setHeader("X-CSRF-TOKEN", cookie.getValue());
            }
        }
    }

    @GET
    @Path("/{id}")
    public Response getComment(@PathParam("id") String id, @QueryParam(CALLBACK_PARAM) String callback) {
        return commentService.getComment(id)
            .map(comment -> wrapResponse(comment, callback))
            .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addComment(@Context HttpServletRequest request, MultivaluedMap<String, String> formParams) {
        var comment = createCommentFromForm(formParams);
        var savedComment = commentService.addComment(comment);

        try {
            var location = new URI(request.getRequestURL() + "/" + savedComment.getId());
            return Response.created(location)
                .entity(savedComment)
                .build();
        } catch (Exception e) {
            log.error("Error creating comment response: {}", e.getMessage());
            throw new WebApplicationException(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GET
    @Path("/summary")
    public Response getPageSummaries(@Context UriInfo uriInfo) {
        var criteria = createCriteriaFromQuery(uriInfo.getQueryParameters());
        var summaries = commentService.getPageSummaries(criteria);
        return wrapResponse(summaries, uriInfo.getQueryParameters().getFirst(CALLBACK_PARAM));
    }

    @PUT
    @Path("/moderate")
    @RolesAllowed("moderator")
    public Response moderateComments(PSCommentIds commentIds, @QueryParam("state") String state) {
        var approvalState = Optional.ofNullable(state)
            .map(String::toUpperCase)
            .map(APPROVAL_STATE::valueOf)
            .orElseThrow(() -> new PSBadRequestException("Invalid approval state"));

        commentIds.getComments().stream()
            .map(id -> commentService.getComment(id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(comment -> moderateComment(comment, approvalState));

        return Response.ok().build();
    }

    private PSCommentCriteria createCriteriaFromQuery(MultivaluedMap<String, String> queryParams) {
        return PSCommentCriteria.builder()
            .site(queryParams.getFirst("site"))
            .pagePath(queryParams.getFirst("pagePath"))
            .maxResults(parseIntParam(queryParams.getFirst("maxResults"), 10))
            .startIndex(parseIntParam(queryParams.getFirst("startIndex"), 0))
            .sortBy(queryParams.getFirst("sortBy"))
            .ascending(parseBooleanParam(queryParams.getFirst("ascending"), true))
            .build();
    }

    private PSRestComment createCommentFromForm(MultivaluedMap<String, String> form) {
        return PSRestComment.builder()
            .text(getRequiredParam(form, "text"))
            .site(getRequiredParam(form, "site"))
            .pagePath(getRequiredParam(form, "pagePath"))
            .username(form.getFirst("username"))
            .email(form.getFirst("email"))
            .url(form.getFirst("url"))
            .title(form.getFirst("title"))
            .createdDate(new Date())
            .tags(parseTags(form.getFirst("tags")))
            .build();
    }

    private String getRequiredParam(MultivaluedMap<String, String> form, String name) {
        return Optional.ofNullable(form.getFirst(name))
            .filter(StringUtils::isNotBlank)
            .orElseThrow(() -> new PSBadRequestException("Missing required parameter: " + name));
    }

    private Set<String> parseTags(String tags) {
        return Optional.ofNullable(tags)
            .filter(StringUtils::isNotBlank)
            .map(t -> t.split(","))
            .map(Arrays::asList)
            .map(list -> list.stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private Response wrapResponse(Object entity, String callback) {
        var response = Response.ok(entity);
        if (StringUtils.isNotBlank(callback)) {
            response.type("application/javascript")
                   .entity(callback + "(" + entity + ");");
        }
        return response.build();
    }

    private int parseIntParam(String value, int defaultValue) {
        return Optional.ofNullable(value)
            .filter(StringUtils::isNotBlank)
            .map(Integer::parseInt)
            .orElse(defaultValue);
    }

    private boolean parseBooleanParam(String value, boolean defaultValue) {
        return Optional.ofNullable(value)
            .filter(StringUtils::isNotBlank)
            .map(Boolean::parseBoolean)
            .orElse(defaultValue);
    }

    private void moderateComment(IPSComment comment, APPROVAL_STATE newState) {
        try {
            commentService.setDefaultModerationState(comment.getSite(), newState);
        } catch (Exception e) {
            log.error("Error moderating comment {}: {}", comment.getId(), e.getMessage());
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
