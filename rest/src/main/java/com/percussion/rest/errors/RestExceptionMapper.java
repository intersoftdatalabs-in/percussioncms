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

package com.percussion.rest.errors;

import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps RestExceptionBase to a proper HTTP response.
 * Sunny Sal: "Exception ko response mein badal diya, boss!"
 */
@Provider
public class RestExceptionMapper implements ExceptionMapper<RestExceptionBase> {

    @Context
    private HttpHeaders headers;

    @Override
    public Response toResponse(RestExceptionBase e) {
        var restError = new RestError(
                e.getErrorCode().getNumVal(),
                e.getClass().getSimpleName(),
                e.getMessage(),
                e.getDetailMessage(),
                e.getErrorData().orElse(null)
        );

        var rb = Response.status(e.getStatus()).entity(restError);

        var accepts = headers.getAcceptableMediaTypes();
        var mt = accepts != null && !accepts.isEmpty()
                ? accepts.stream()
                        .filter(accept -> MediaType.APPLICATION_JSON_TYPE.equals(accept)
                                || MediaType.APPLICATION_XML_TYPE.equals(accept))
                        .findFirst()
                        .orElse(MediaType.APPLICATION_XML_TYPE)
                : MediaType.APPLICATION_XML_TYPE;

        rb = rb.type(mt); // Set the response type to the entity type.

        return rb.build();
    }
}
