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
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link JsonMappingException} to a 500 server error response.
 *
 * <p>Sunny Sal says: JSON mapping failed? Time to check your object graph!
 */
@Provider
@Priority(1)
public class PSJsonMappingErrorResponse implements ExceptionMapper<JsonMappingException> {

    private static final Logger log = LogManager.getLogger(PSJsonMappingErrorResponse.class);

    /**
     * Maps a {@link JsonMappingException} to a plain text 500 error response.
     *
     * @param exception the exception to map to a response
     * @return a response mapped from the supplied exception
     */
    @Override
    public Response toResponse(JsonMappingException exception) {
        log.error(PSExceptionUtils.getMessageForLog(exception));
        log.debug(PSExceptionUtils.getDebugMessageForLog(exception));
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("A server error happened. Please try your request again.")
                .type("text/plain")
                .build();
    }
}
