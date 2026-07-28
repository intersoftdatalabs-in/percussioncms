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

package com.percussion.delivery.jaxrs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default {@link ExceptionMapper} used by the delivery tier services. Recognized JSON parsing or
 * mapping errors are reported as 400 Bad Request; existing {@link WebApplicationException}s are
 * passed through; everything else is surfaced as a 500 Internal Server Error.
 */
@Provider
public class PSExceptionMapper implements ExceptionMapper<Exception> {

  /** Default constructor. */
  public PSExceptionMapper() {}

  private static final Logger log = LogManager.getLogger(PSExceptionMapper.class);

  @Context private HttpServletRequest request;

  /**
   * Maps an exception to a JAX-RS {@link Response}. Recognized JSON parsing/mapping errors are
   * reported as 400 Bad Request. Other exceptions fall through to a 500 Internal Server Error.
   * Existing {@link WebApplicationException} responses are returned unchanged.
   *
   * @param e the exception to map, never <code>null</code>.
   * @return a JAX-RS response describing the error.
   */
  @Override
  public Response toResponse(Exception e) {

    String clientMsg;
    Status status;
    if (e instanceof JsonMappingException) {
      clientMsg = "Invalid request. JSON property is not of an expected type";
      status = Response.Status.BAD_REQUEST;
    } else if (e instanceof JsonParseException) {
      clientMsg = "Invalid request.  Invalid JSON object";
      status = Response.Status.BAD_REQUEST;
    } else if (e instanceof WebApplicationException) {
      log.debug("WebApplicationException:{}", e.getMessage(), e);
      return ((WebApplicationException) e).getResponse();
    } else {
      clientMsg = "An unexpected error occurred processing the request on the DTS server";
      status = Status.INTERNAL_SERVER_ERROR;
    }

    log.error(PSExceptionUtils.getMessageForLog(e));
    log.debug(PSExceptionUtils.getDebugMessageForLog(e));

    return Response.status(status).entity(clientMsg).type(MediaType.TEXT_PLAIN).build();
  }
}
