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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.percussion.delivery.polls.data.PSPollsResponse;
import com.percussion.delivery.polls.data.PSRestPoll;
import com.percussion.delivery.services.IPSRestService;

/**
 * 
 * @author natechadwick
 *
 */
// REFACTORED: CP-JAVA11
public interface IPSPollsRestService extends IPSRestService {
    @GET
    @Path("/{pollName}")
    @Produces(MediaType.APPLICATION_JSON)
    PSPollsResponse getPoll(@PathParam("pollName") String pollName);

    @GET
    @Path("/question/{pollQuestion}")
    @Produces(MediaType.APPLICATION_JSON)
    PSPollsResponse getPollByQuestion(@PathParam("pollQuestion") String pollQuestion);

    @PUT
    @Path("/save")
    @Produces(MediaType.APPLICATION_JSON)
    PSPollsResponse savePoll(PSRestPoll restPoll, @Context HttpServletRequest req);

    @GET
    @Path("/canuservote/{pollQuestion}")
    @Produces(MediaType.APPLICATION_JSON)
    String canUserVote(@PathParam("pollQuestion") String pollQuestion, @Context HttpServletRequest req);
}
