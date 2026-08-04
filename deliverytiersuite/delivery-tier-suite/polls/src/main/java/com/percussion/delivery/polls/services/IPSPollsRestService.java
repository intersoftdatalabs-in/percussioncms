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

import com.percussion.delivery.polls.data.PSPollsResponse;
import com.percussion.delivery.polls.data.PSRestPoll;
import com.percussion.delivery.services.IPSRestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

// REFACTORED: CP-JAVA11
/**
 * REST contract for the polls feature. Exposes the JSON endpoints that allow clients to look up a
 * poll by name or by question and to record new votes, and to query whether a user is allowed to
 * vote in the current session.
 *
 * @author natechadwick
 */
public interface IPSPollsRestService extends IPSRestService {
  /**
   * Looks up a poll by its name.
   *
   * @param pollName the poll name supplied as a path parameter, not {@code null}.
   * @return a {@link PSPollsResponse} with status SUCCESS and the matching {@link PSRestPoll} as
   *     the result, or status ERROR with an explanatory message when no poll is found.
   */
  @GET
  @Path("/{pollName}")
  @Produces(MediaType.APPLICATION_JSON)
  PSPollsResponse getPoll(@PathParam("pollName") String pollName);

  /**
   * Looks up a poll by its question text.
   *
   * @param pollQuestion the poll question supplied as a path parameter, not {@code null}.
   * @return a {@link PSPollsResponse} with status SUCCESS and the matching {@link PSRestPoll} as
   *     the result, or status ERROR with an explanatory message when no poll is found.
   */
  @GET
  @Path("/question/{pollQuestion}")
  @Produces(MediaType.APPLICATION_JSON)
  PSPollsResponse getPollByQuestion(@PathParam("pollQuestion") String pollQuestion);

  /**
   * Records the supplied poll votes against the supplied poll and returns the updated poll.
   *
   * @param restPoll the payload describing the poll and the answers selected, not {@code null}.
   * @param req the current HTTP servlet request, not {@code null}; consulted when the poll is
   *     restricted to one submission per session.
   * @return a {@link PSPollsResponse} containing the updated poll on success, or status ERROR on
   *     failure.
   */
  @PUT
  @Path("/save")
  @Produces(MediaType.APPLICATION_JSON)
  PSPollsResponse savePoll(PSRestPoll restPoll, @Context HttpServletRequest req);

  /**
   * Reports whether the calling user is allowed to vote in the current session for the supplied
   * poll question.
   *
   * @param pollQuestion the poll question supplied as a path parameter, not {@code null}.
   * @param req the current HTTP servlet request, not {@code null}; used to look up the session and
   *     any previous "already voted" marker.
   * @return the string {@code "true"} when the user may vote, {@code "false"} when the user has
   *     already voted in this session.
   */
  @GET
  @Path("/canuservote/{pollQuestion}")
  @Produces(MediaType.APPLICATION_JSON)
  String canUserVote(
      @PathParam("pollQuestion") String pollQuestion, @Context HttpServletRequest req);
}
