/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.delivery.polls.services;

import com.percussion.delivery.polls.data.IPSPoll;
import com.percussion.delivery.polls.data.PSPollsResponse;
import com.percussion.delivery.polls.data.PSPollsResponse.PollResponseStatus;
import com.percussion.delivery.polls.data.PSRestPoll;
import com.percussion.delivery.services.PSAbstractRestService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** REST service for polls feature implementation. */
@Path("/polls")
@Component
// REFACTORED: CP-JAVA11
public class PSPollsRestService extends PSAbstractRestService implements IPSPollsRestService {
  private static final String SERVER_ERROR_MESSAGE =
      "Failed to process your request due to an unexpected error.";
  private static final Logger log = LogManager.getLogger(PSPollsRestService.class);
  private IPSPollsService pollsService;

  /** Default constructor required for Spring component-scanned instantiation paths. */
  public PSPollsRestService() {}

  /**
   * Spring-injected constructor.
   *
   * @param pollsService the polls service to delegate business operations to, not {@code null}.
   */
  @Autowired
  public PSPollsRestService(IPSPollsService pollsService) {
    this.pollsService = pollsService;
  }

  /**
   * HEAD endpoint that echoes an {@code XSRF-TOKEN} cookie as a header pair ({@code X-CSRF-HEADER}
   * → {@code X-XSRF-TOKEN} and {@code X-CSRF-TOKEN} → cookie value).
   *
   * @param request the current HTTP servlet request, not {@code null}.
   * @param response the current HTTP servlet response, not {@code null}.
   */
  @HEAD
  @Path("/csrf")
  public void csrf(@Context HttpServletRequest request, @Context HttpServletResponse response) {
    var cookies = request.getCookies();
    if (cookies == null) {
      return;
    }
    for (var cookie : cookies) {
      if ("XSRF-TOKEN".equals(cookie.getName())) {
        response.setHeader("X-CSRF-HEADER", "X-XSRF-TOKEN");
        response.setHeader("X-CSRF-TOKEN", cookie.getValue());
      }
    }
  }

  @Override
  @GET
  @Path("/{pollName}")
  @Produces(MediaType.APPLICATION_JSON)
  public PSPollsResponse getPoll(@PathParam("pollName") String pollName) {
    try {
      var poll = pollsService.findPoll(pollName);
      if (poll == null) {
        return new PSPollsResponse(
            PollResponseStatus.ERROR, "No results found for poll with name: " + pollName);
      }
      var restPoll = convertToRestPoll(poll);
      return new PSPollsResponse(PollResponseStatus.SUCCESS, restPoll);
    } catch (Exception t) {
      log.error(
          "Error occurred while getting poll by name: {}, Error: {}",
          t.getLocalizedMessage(),
          t.getMessage());
      log.debug(t.getMessage(), t);
      return new PSPollsResponse(PollResponseStatus.ERROR, SERVER_ERROR_MESSAGE);
    }
  }

  @Override
  @GET
  @Path("/question/{pollQuestion}")
  @Produces(MediaType.APPLICATION_JSON)
  public PSPollsResponse getPollByQuestion(@PathParam("pollQuestion") String pollQuestion) {
    if (log.isDebugEnabled()) {
      log.debug("Poll question is: {}", pollQuestion);
    }
    try {
      var poll = pollsService.findPollByQuestion(pollQuestion);
      if (poll == null) {
        return new PSPollsResponse(
            PollResponseStatus.ERROR, "No results found for poll with question: " + pollQuestion);
      }
      var restPoll = convertToRestPoll(poll);
      return new PSPollsResponse(PollResponseStatus.SUCCESS, restPoll);
    } catch (Exception t) {
      log.error(
          "Error occurred while getting poll by question: {}, Error: {}",
          t.getLocalizedMessage(),
          t.getMessage());
      log.debug(t.getMessage(), t);
      return new PSPollsResponse(PollResponseStatus.ERROR, SERVER_ERROR_MESSAGE);
    }
  }

  @Override
  @POST
  @Path("/save")
  @Produces(MediaType.APPLICATION_JSON)
  public PSPollsResponse savePoll(PSRestPoll restPoll, @Context HttpServletRequest req) {
    if (log.isDebugEnabled()) {
      log.debug("Context path in http servlet request is: {}", req.getContextPath());
    }
    try {
      pollsService.savePoll(
          restPoll.getPollName(), restPoll.getPollQuestion(), restPoll.getPollSubmits());
      var poll = pollsService.findPollByQuestion(restPoll.getPollQuestion());
      if (restPoll.isRestrictBySession()) {
        var session = req.getSession(true);
        session.setAttribute(restPoll.getPollQuestion(), "true");
      }
      var updatedRestPoll = convertToRestPoll(poll);
      return new PSPollsResponse(PollResponseStatus.SUCCESS, updatedRestPoll);
    } catch (Exception t) {
      log.error(
          "Error occurred while saving a poll ({}): {}, Error: {}",
          restPoll.getPollName(),
          t.getLocalizedMessage(),
          t.getMessage());
      log.debug(t.getMessage(), t);
      return new PSPollsResponse(PollResponseStatus.ERROR, SERVER_ERROR_MESSAGE);
    }
  }

  @Override
  @GET
  @Path("/canuservote/{pollQuestion}")
  @Produces(MediaType.APPLICATION_JSON)
  public String canUserVote(
      @PathParam("pollQuestion") String pollQuestion, @Context HttpServletRequest req) {
    if (log.isDebugEnabled()) {
      log.debug("Context path in http servlet request is: {}", req.getContextPath());
    }
    var session = req.getSession(true);
    var sessVar = session.getAttribute(pollQuestion);
    return (sessVar == null) ? "true" : "false";
  }

  /**
   * Converts the supplied domain poll into the REST representation used by the polls REST API.
   *
   * @param poll the source poll, not {@code null}.
   * @return the converted REST poll, never {@code null}.
   */
  private PSRestPoll convertToRestPoll(IPSPoll poll) {
    var restPoll = new PSRestPoll();
    restPoll.setPollName(poll.getPollName());
    restPoll.setPollQuestion(poll.getPollQuestion());
    var results = new HashMap<String, Integer>();
    var totalVotes = 0;
    for (var pollAnswer : poll.getPollAnswers()) {
      totalVotes += pollAnswer.getCount();
      results.put(pollAnswer.getAnswer(), pollAnswer.getCount());
    }
    restPoll.setPollResults(results);
    restPoll.setTotalVotes(totalVotes);
    return restPoll;
  }

  /**
   * Returns the polls module's reported version, logging it on the way through.
   *
   * @return the polls module version string.
   */
  public String getVersion() {
    var version = super.getVersion();
    log.info("getVersion() from PSPollsRestService ...{}", version);
    return version;
  }

  /** {@inheritDoc} */
  @Override
  @DELETE
  @Path("/updateOldSiteEntries/{prevSiteName}/{newSiteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("deliverymanager")
  public Response updateOldSiteEntries(
      @PathParam("prevSiteName") String prevSiteName,
      @PathParam("newSiteName") String newSiteName) {
    log.debug("Polls service for site rename. Nothing to do for site: {}", prevSiteName);
    // No operation needed for polls on site rename.
    return Response.status(Response.Status.NO_CONTENT).build();
  }
}
