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

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSCommentIds;
import com.percussion.delivery.comments.data.PSCommentSort;
import com.percussion.delivery.comments.data.PSCommentSort.SORTBY;
import com.percussion.delivery.comments.data.PSComments;
import com.percussion.delivery.comments.data.PSPageSummaries;
import com.percussion.delivery.comments.data.PSRestComment;
import com.percussion.delivery.comments.service.rdbms.PSComment;
import com.percussion.delivery.exceptions.PSBadRequestException;
import com.percussion.delivery.services.PSAbstractRestService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.utils.PSRedirectValidation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.InternalServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST/Webservice layer used to access the comments service.
 *
 * @author erikserating
 */
@Path("/comment")
@Component
@Consumes({"application/xml", "application/json"})
public class PSCommentsRestService extends PSAbstractRestService implements IPSCommentRestService {

  private static final String CALLBACK_FN = "_jqjsp";
  private static final Logger log = LogManager.getLogger(PSCommentsRestService.class);
  private static final String iso8601ExtendedString = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

  private final IPSCommentsService commentService;

  /**
   * Creates a new comments REST service backed by the supplied service.
   *
   * @param service the underlying comments service, must not be {@code null}.
   */
  @Inject
  @Autowired
  public PSCommentsRestService(IPSCommentsService service) {
    this.commentService = service;
  }

  /**
   * Issues a CSRF token response header derived from the {@code XSRF-TOKEN} cookie if present. Used
   * by the comment form to satisfy CSRF protection.
   *
   * @param request the HTTP request.
   * @param response the HTTP response, which receives the CSRF headers when applicable.
   */
  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (Cookie cookie : cookies) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
        response.setHeader("X-CSRF-TOKEN", cookie.getValue());
      }
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#getComments(com.percussion.delivery.comments.data.PSCommentCriteria)
   */
  @Override
  @POST
  @Path("/list")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public PSComments getComments(PSCommentCriteria criteria) {
    if (criteria == null) {
      PSCommentsRestService.log.error("criteria cannot be null.");
      throw new IllegalArgumentException("criteria cannot be null.");
    }

    if (PSCommentsRestService.log.isDebugEnabled()) {
      PSCommentsRestService.log.debug("Criteria in the service is : {}", criteria.toJSON());
    }
    try {
      return this.toRestComments(this.commentService.getComments(criteria, false));
    } catch (Exception e) {
      PSCommentsRestService.log.error(
          "Exception occurred while getting comments!, Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));

      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#getCommentsAsModerator(com.percussion.delivery.comments.data.PSCommentCriteria)
   */
  @Override
  @POST
  @RolesAllowed("deliverymanager")
  @Path("/moderation/asmoderator")
  @Produces("application/json")
  public PSComments getCommentsAsModerator(PSCommentCriteria criteria) {
    if (criteria == null) {
      PSCommentsRestService.log.error("criteria cannot be null.");
      throw new IllegalArgumentException("criteria cannot be null.");
    }
    if (PSCommentsRestService.log.isDebugEnabled()) {
      PSCommentsRestService.log.debug("Criteria in the service is :{}", criteria.toJSON());
    }
    try {
      return this.toRestComments(this.commentService.getComments(criteria, true));
    } catch (Exception e) {
      PSCommentsRestService.log.error(
          "Exception occurred while getting comments as moderator!, Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));

      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#getCommentsP(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @POST
  @Path("/jsonp")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public GenericEntity getCommentsP(PSCommentCriteria criteria) {
    try {
      if (criteria.getCallback() == null || criteria.getCallback().isEmpty())
        criteria.setCallback(PSCommentsRestService.CALLBACK_FN);

      if (StringUtils.isNotBlank(criteria.getSortby())) {
        boolean asc =
            StringUtils.isNotBlank(criteria.getAscending())
                && "true".equalsIgnoreCase(criteria.getAscending());
        PSCommentSort commentSort = new PSCommentSort(this.getSortBy(criteria.getSortby()), asc);
        criteria.setSort(commentSort);
      }

      this.validateCallback(criteria.getCallback());

      PSComments results = this.getComments(criteria);
      for (IPSComment ipc : results.getComments()) {
        String cDate =
            FastDateFormat.getInstance(PSCommentsRestService.iso8601ExtendedString)
                .format(ipc.getCreatedDate());
        ipc.setCommentCreatedDate(cDate);
      }

      return new GenericEntity<PSComments>(results) {};

    } catch (IllegalArgumentException e) {
      PSCommentsRestService.log.error(
          "Illegal Argument Exception!, Error: {}", PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSBadRequestException(e.getLocalizedMessage());
    }
  }

  private void validateCallback(String callback) {
    if (!PSCommentsRestService.CALLBACK_FN.equals(callback))
      throw new IllegalArgumentException("Invalid callback parameter supplied");
  }

  /**
   * @param sortby
   */
  private SORTBY getSortBy(String sortby) {
    PSCommentSort.SORTBY[] sortvals = PSCommentSort.SORTBY.values();
    for (SORTBY sortbyval : sortvals) {
      if (sortbyval.name().equals(sortby)) return PSCommentSort.SORTBY.valueOf(sortby);
    }

    PSCommentsRestService.log.error(
        "Illegal Argument Exception! : Invalid sortby parameter supplied.");
    throw new IllegalArgumentException("Invalid sortby parameter supplied");
  }

  /**
   * @param strInt
   * @param paramName
   */
  private int getIntValue(String strInt, String paramName) {
    try {
      return Integer.parseInt(strInt);
    } catch (NumberFormatException e) {
      PSCommentsRestService.log.error(
          "Number Format Exception! - Invalid {} parameter supplied, Error: {}",
          paramName,
          PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new IllegalArgumentException("Invalid " + paramName + " parameter supplied");
    }
  }

  /**
   * @param state
   */
  private APPROVAL_STATE getState(String state) {
    APPROVAL_STATE[] approvalVals = APPROVAL_STATE.values();
    for (APPROVAL_STATE approvalVal : approvalVals) {
      if (approvalVal.name().equals(state)) return APPROVAL_STATE.valueOf(state);
    }

    PSCommentsRestService.log.error("Illegal Argument Exception! - Invalid parameter supplied.");
    throw new IllegalArgumentException("Invalid state parameter supplied");
  }

  /*
   * (non-Javadoc)
   *
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#
   * getPagesWithComments(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  @POST
  @Path("/pageswithcomments/{site}")
  @Produces("application/json")
  public PSPageSummaries getPagesWithComments(
      @PathParam("site") String site, PSCommentCriteria criteria) {
    int max = -1;
    if (criteria != null && criteria.getMaxResults() > 0) {
      try {
        max = criteria.getMaxResults();
      } catch (NumberFormatException ignore) {
        PSCommentsRestService.log.error("Number Format Exception!, Error: {}", ignore.getMessage());
        PSCommentsRestService.log.debug(ignore.getMessage(), ignore);
      }
    }
    int start = -1;
    if (criteria != null && criteria.getStartIndex() > 0) {
      try {
        start = criteria.getStartIndex();
      } catch (NumberFormatException ignore) {
        PSCommentsRestService.log.error("Number Format Exception!, Error: {}", ignore.getMessage());
        PSCommentsRestService.log.debug(ignore.getMessage(), ignore);
      }
    }
    try {
      return this.commentService.getPagesWithComments(site, max, start);
    } catch (Exception e) {
      PSCommentsRestService.log.error(
          "Exception occurred while getting pages with comments, Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#addComment(jakarta.ws.rs.core.MultivaluedMap, java.lang.String, jakarta.ws.rs.core.HttpHeaders)
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Path("/addcomment")
  @Override
  public Response addComment(
      @Context ContainerRequest containerRequest,
      @FormParam("action") String action,
      @Context HttpHeaders headers) {
    MultivaluedMap<String, String> headerParams = headers.getRequestHeaders();
    Form form = (Form) containerRequest.getProperty(InternalServerProperties.FORM_DECODED_PROPERTY);
    MultivaluedMap<String, String> params = form.asMap();
    PSRestComment comment = new PSRestComment();
    comment.setUsername(params.getFirst(IPSCommentRestService.FORM_PARAM_USERNAME));
    comment.setPagePath(params.getFirst(IPSCommentRestService.FORM_PARAM_PAGEPATH));
    comment.setSite(params.getFirst(IPSCommentRestService.FORM_PARAM_SITE));
    comment.setEmail(params.getFirst(IPSCommentRestService.FORM_PARAM_EMAIL));
    comment.setText(params.getFirst(IPSCommentRestService.FORM_PARAM_TEXT));
    comment.setUrl(params.getFirst(IPSCommentRestService.FORM_PARAM_URL));
    comment.setTitle(params.getFirst(IPSCommentRestService.FORM_PARAM_TITLE));
    List<String> tags = params.get(IPSCommentRestService.FORM_PARAM_TAGS);
    if (StringUtils.isBlank(comment.getPagePath()) || StringUtils.isBlank(comment.getSite())) {
      PSCommentsRestService.log.error("pagepath or site cannot be null or empty.");
      throw new WebApplicationException(
          new IllegalArgumentException("pagepath and site cannot be null or empty"),
          Response.serverError().build());
    }

    /**
     * if the hidden Honeypot field isn't empty, it was likely filled out by a robot. we can return
     * a successful response to indicate normal behavior to trick the bot.
     */
    if (!StringUtils.isBlank(params.getFirst(IPSCommentRestService.FORM_PARAM_HONEYPOT))) {
      PSCommentsRestService.log.debug(
          "Detected hidden honeypot field was filled out.  Ignoring comment -- see request headers"
              + " below.");
      String referer = headerParams.getFirst("Referer");
      // CWE-601 / java/unvalidated-url-redirection #643: Referer is attacker-controlled.
      return seeOtherIfSafe(referer, headerParams);
    }

    if (PSCommentsRestService.log.isDebugEnabled()) {
      PSCommentsRestService.log.debug(
          "Http Header in the service is : {}", headers.getRequestHeaders());
    }

    if (tags != null && tags.size() > 0) {
      comment.setTags(new HashSet<>(params.get(IPSCommentRestService.FORM_PARAM_TAGS)));
    }
    try {

      PSComment comm = new PSComment(comment);
      IPSComment newComment = this.commentService.addComment(comment);
      String referer = headerParams.getFirst("Referer");
      if (referer != null
          && comment.getPagePath() != null
          && !referer.contains(comment.getPagePath())) {
        if (referer.endsWith("/") && comment.getPagePath().startsWith("/")) {
          referer = referer.substring(0, referer.length() - 1);
        }
        referer = referer + comment.getPagePath();
      }
      if (referer != null && referer.contains("?lastCommentId")) {
        int commentIndex = referer.indexOf("?lastCommentId");
        referer = referer.substring(0, commentIndex);
      }
      String target = referer + "?lastCommentId=" + newComment.getId();
      // CWE-601 / java/unvalidated-url-redirection #644: validate final redirect target.
      return seeOtherIfSafe(target, headerParams);
    } catch (Exception e) {
      PSCommentsRestService.log.error(
          "Exception occurred while adding comment, Error: {}",
          PSExceptionUtils.getMessageForLog(e));
      PSCommentsRestService.log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(e, Response.serverError().build());
    }
  }

  /**
   * Returns {@code 303 See Other} only for a <em>relative</em> redirect. Absolute Referer URLs are
   * reduced to path+query+fragment so the client Host header cannot force an off-site location
   * (CWE-601 / #643/#644 review: do not trust Host as an allow-list).
   */
  private static Response seeOtherIfSafe(String target, MultivaluedMap<String, String> headers) {
    if (StringUtils.isBlank(target)) {
      return Response.noContent().build();
    }
    String relative = toRelativeRedirectTarget(target);
    String safe =
        relative == null ? null : PSRedirectValidation.validateInternalRedirectUrl(relative);
    if (safe == null) {
      PSCommentsRestService.log.warn(
          "Rejected unvalidated redirect target: {}", sanitizeForLog(target));
      return Response.noContent().build();
    }
    try {
      // Rebuild a relative URI from validated components only so CodeQL does not
      // treat the post-validation string as still user-tainted (#1778 residual).
      URI rebuilt = rebuildRelativeRedirectUri(safe);
      return Response.seeOther(rebuilt).build();
    } catch (URISyntaxException e) {
      PSCommentsRestService.log.error(
          "Error creating redirect URI, Error: {}", PSExceptionUtils.getMessageForLog(e));
      return Response.noContent().build();
    }
  }

  /**
   * Builds a scheme-less relative {@link URI} from a previously validated path/query/fragment
   * string. Only raw path/query/fragment components are used.
   */
  static URI rebuildRelativeRedirectUri(String validatedRelative) throws URISyntaxException {
    if (validatedRelative == null || validatedRelative.startsWith("//")) {
      throw new URISyntaxException(
          validatedRelative == null ? "null" : validatedRelative, "expected absolute path");
    }
    URI parsed = new URI(validatedRelative);
    // Protocol-relative / authority forms are not relative paths
    if (parsed.getScheme() != null || parsed.getRawAuthority() != null) {
      throw new URISyntaxException(validatedRelative, "expected absolute path without authority");
    }
    String path = parsed.getRawPath();
    if (path == null || path.isEmpty()) {
      path = "/";
    }
    if (!path.startsWith("/") || path.startsWith("//")) {
      throw new URISyntaxException(validatedRelative, "expected absolute path");
    }
    return new URI(null, null, path, parsed.getRawQuery(), parsed.getRawFragment());
  }

  /**
   * Converts an absolute URL to a same-document relative path (path + query + fragment). Protocol-
   * relative and non-http(s) absolute URLs are rejected ({@code null}).
   */
  static String toRelativeRedirectTarget(String target) {
    try {
      URI uri = new URI(target.trim());
      if (!uri.isAbsolute()) {
        if (target.startsWith("/") && !target.startsWith("//")) {
          return target;
        }
        return null;
      }
      String scheme = uri.getScheme();
      if (scheme != null && !"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        return null;
      }
      String path = uri.getRawPath();
      if (path == null || path.isEmpty()) {
        path = "/";
      }
      StringBuilder b = new StringBuilder(path);
      if (uri.getRawQuery() != null) {
        b.append('?').append(uri.getRawQuery());
      }
      if (uri.getRawFragment() != null) {
        b.append('#').append(uri.getRawFragment());
      }
      return b.toString();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Strip all Unicode control characters (including CR/LF/TAB) before logging so crafted Referer
   * values cannot inject new log lines (log injection hygiene).
   */
  static String sanitizeForLog(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[\\p{Cntrl}]", "").trim();
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#delete(com.percussion.delivery.comments.data.PSCommentIds)
   */
  @Override
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/delete")
  public void delete(PSCommentIds commentIds) {
    try {
      this.commentService.deleteComments(commentIds.getComments());
    } catch (Exception ex) {
      PSCommentsRestService.log.error(
          "Exception occurred while deleting, Error: {}", ex.getMessage());
      PSCommentsRestService.log.debug(ex.getMessage(), ex);
      throw new WebApplicationException(ex, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#approve(com.percussion.delivery.comments.data.PSCommentIds)
   */
  @Override
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/approve")
  public void approve(PSCommentIds commentIds) {
    try {
      this.commentService.approveComments(commentIds.getComments());
    } catch (Exception ex) {
      PSCommentsRestService.log.error(
          "Exception occurred while approving comments, Error: {}", ex.getMessage());
      PSCommentsRestService.log.debug(ex.getMessage(), ex);
      throw new WebApplicationException(ex, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#reject(com.percussion.delivery.comments.data.PSCommentIds)
   */
  @Override
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/reject")
  public void reject(PSCommentIds commentIds) {
    try {
      this.commentService.rejectComments(commentIds.getComments());
    } catch (Exception ex) {
      PSCommentsRestService.log.error(
          "Exception occurred while rejecting comments, Error: {}", ex.getMessage());
      PSCommentsRestService.log.debug(ex.getMessage(), ex);
      throw new WebApplicationException(ex, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#setDefaultModerationState(java.util.Map)
   */
  @Override
  @PUT
  @RolesAllowed("deliverymanager")
  @Path("/moderation/defaultModerationState")
  public void setDefaultModerationState(Map data) {
    try {
      String site = (String) data.get("site");
      String state = (String) data.get("state");
      if (PSCommentsRestService.log.isDebugEnabled()) {
        PSCommentsRestService.log.debug(
            "Site in the map data is : {} and state in the map data is : {}", site, state);
      }

      this.commentService.setDefaultModerationState(site, APPROVAL_STATE.valueOf(state));
    } catch (Exception ex) {
      PSCommentsRestService.log.error(
          "Exception occurred while setting default moderation state, Error: {}", ex.getMessage());
      PSCommentsRestService.log.debug(ex.getMessage(), ex);
      throw new WebApplicationException(ex, Response.serverError().build());
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.delivery.comments.services.IPSCommentRestService#getDefaultModerationState(java.lang.String)
   */
  @Override
  @GET
  @Path("/defaultModerationState/{site}")
  public String getDefaultModerationState(@PathParam("site") String site) {
    try {
      return this.commentService.getDefaultModerationState(site).toString();
    } catch (Exception ex) {
      PSCommentsRestService.log.error(
          "Exception occurred while getting default moderation state, Error: {}", ex.getMessage());
      PSCommentsRestService.log.debug(ex.getMessage(), ex);
      throw new WebApplicationException(ex, Response.serverError().build());
    }
  }

  /**
   * Ensures comments are <code>PSRestComment</code> object instances.
   *
   * @param comments never <code>null</code>, may be empty.
   * @return comments with only <code>PSRestComment</code> instances. Never <code>null</code>, may
   *     be empty.
   */
  private PSComments toRestComments(PSComments comments) {
    PSComments results = new PSComments();
    for (IPSComment com : comments.getComments()) {
      if (com instanceof PSRestComment) {
        results.getComments().add(com);
      } else {
        results.getComments().add(new PSRestComment(com));
      }
    }
    return results;
  }

  @Override
  public String getVersion() {

    String version = super.getVersion();

    PSCommentsRestService.log.info("getVersion() from PSCommentsRestService ...{}", version);

    return version;
  }

  @DELETE
  @Path("/updateOldSiteEntries/{prevSiteName}/{newSiteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  @Override
  public Response updateOldSiteEntries(String prevSiteName, String newSiteName) {
    PSCommentsRestService.log.info("Attempting to update comments for site name: {}", prevSiteName);
    boolean result = this.commentService.updateCommentsForRenameSite(prevSiteName, newSiteName);
    if (!result) {
      PSCommentsRestService.log.error("Error updating comments for site: {}", prevSiteName);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
