/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.recycle.service.impl;

import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.recycle.service.IPSEmptyRecycleService;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleException;
import com.percussion.recycle.service.IPSEmptyRecycleService.PSEmptyRecycleNotAuthorizedException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * CM1 internal REST for Recycling bin operations used by the WebUI.
 *
 * <p>Mounted under pathmanagement at {@code /pathmanagement/recycle/*} (see sitemanage-beans
 * jaxrs:server). Not a public {@code rest} module adaptor surface.
 *
 * <p>Permissions: emptying the bin requires Admin (enforced in {@link IPSEmptyRecycleService}).
 */
@Path("/recycle")
@Component("recycleRestService")
@Lazy
public class PSRecycleRestService {

  private final IPSEmptyRecycleService emptyRecycleService;

  @Autowired
  public PSRecycleRestService(IPSEmptyRecycleService emptyRecycleService) {
    this.emptyRecycleService = emptyRecycleService;
  }

  /**
   * Permanently empties the system Recycling bin.
   *
   * <p>DELETE is the primary verb (destructive purge). POST is accepted as an alias for clients
   * that cannot send DELETE with a body-less request reliably.
   *
   * @return summary of the empty operation (never HTTP 204 — always a result body)
   */
  @DELETE
  @Path("/empty")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEmptyRecycleResult emptyRecyclingBinDelete() {
    return emptyRecyclingBin();
  }

  /**
   * POST alias for {@link #emptyRecyclingBinDelete()}.
   *
   * @return summary of the empty operation
   */
  @POST
  @Path("/empty")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public PSEmptyRecycleResult emptyRecyclingBinPost() {
    return emptyRecyclingBin();
  }

  private PSEmptyRecycleResult emptyRecyclingBin() {
    try {
      return emptyRecycleService.emptyRecyclingBin();
    } catch (PSEmptyRecycleNotAuthorizedException e) {
      log.warn("Empty Recycling denied: {}", PSExceptionUtils.getMessageForLog(e));
      throw new WebApplicationException(
          Response.status(Response.Status.FORBIDDEN)
              .entity("Only Admin users may empty the Recycling bin.")
              .type(MediaType.TEXT_PLAIN)
              .build());
    } catch (PSEmptyRecycleException e) {
      log.error("Empty Recycling failed: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      // Do not leak internal exception details to the client (CWE-209)
      throw new WebApplicationException(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Failed to empty the Recycling bin.")
              .type(MediaType.TEXT_PLAIN)
              .build());
    } catch (PSDataServiceException e) {
      log.error("Empty Recycling data error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new WebApplicationException(
          Response.status(Response.Status.INTERNAL_SERVER_ERROR)
              .entity("Failed to empty the Recycling bin.")
              .type(MediaType.TEXT_PLAIN)
              .build());
    }
  }

  private static final Logger log = LogManager.getLogger(PSRecycleRestService.class);
}
