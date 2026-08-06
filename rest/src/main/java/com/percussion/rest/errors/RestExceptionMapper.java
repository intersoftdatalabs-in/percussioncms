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

// REFACTORED: CP-JAVA11

package com.percussion.rest.errors;

import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps RestExceptionBase to a proper HTTP response. Sunny Sal: "Exception ko response mein badal
 * diya, boss!"
 *
 * <p>Peer of {@link WebApplicationExceptionMapper} for bare {@code WebApplicationException} throws.
 * Registered as a CXF/Spring provider bean ({@code restExceptionMapper}).
 */
@Provider
@PSSiteManageBean("restExceptionMapper")
public class RestExceptionMapper implements ExceptionMapper<RestExceptionBase> {

  @Context private HttpHeaders headers;

  @Override
  public Response toResponse(RestExceptionBase e) {
    var code =
        e.getErrorCode() != null ? e.getErrorCode().getNumVal() : RestErrorCode.OTHER.getNumVal();
    var restError =
        new RestError(
            code,
            e.getClass().getSimpleName(),
            e.getMessage(),
            e.getDetailMessage(),
            e.getErrorData().orElse(null));

    int status =
        e.getStatus() != null
            ? e.getStatus().getStatusCode()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    var rb = Response.status(status).entity(restError);

    var mt = resolveMediaType();
    rb = rb.type(mt);

    return rb.build();
  }

  private MediaType resolveMediaType() {
    if (headers == null) {
      return MediaType.APPLICATION_JSON_TYPE;
    }
    var accepts = headers.getAcceptableMediaTypes();
    if (accepts == null || accepts.isEmpty()) {
      return MediaType.APPLICATION_JSON_TYPE;
    }
    return accepts.stream()
        .filter(
            accept ->
                MediaType.APPLICATION_JSON_TYPE.isCompatible(accept)
                    || MediaType.APPLICATION_XML_TYPE.isCompatible(accept))
        .findFirst()
        .orElse(MediaType.APPLICATION_JSON_TYPE);
  }
}
