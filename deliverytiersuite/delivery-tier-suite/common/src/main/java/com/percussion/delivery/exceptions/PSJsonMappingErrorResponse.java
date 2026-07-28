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

package com.percussion.delivery.exceptions;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.percussion.security.error.PSExceptionUtils;
import jakarta.annotation.Priority;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Maps Jackson {@link JsonMappingException}s into a generic 500 plain-text response, logging the
 * original error for diagnostics.
 */
@Provider
@Priority(1)
public class PSJsonMappingErrorResponse implements ExceptionMapper<JsonMappingException> {

  /** Default constructor. */
  public PSJsonMappingErrorResponse() {}

  private static Logger log = LogManager.getLogger(PSJsonMappingErrorResponse.class);

  /**
   * Map an exception to a {@link Response}. Returning {@code null} results in a {@link
   * Response.Status#NO_CONTENT} response. Throwing a runtime exception results in a {@link
   * Response.Status#INTERNAL_SERVER_ERROR} response.
   *
   * @param exception the exception to map to a response.
   * @return a response mapped from the supplied exception.
   */
  @Override
  public Response toResponse(JsonMappingException exception) {
    log.error(PSExceptionUtils.getMessageForLog(exception));
    log.debug(PSExceptionUtils.getDebugMessageForLog(exception));
    return Response.status(500)
        .entity("A server error happened. Please try your request again.")
        .type("text/plain")
        .build();
  }
}
