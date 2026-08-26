// REFACTORED: CP-JAVA11
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

package com.percussion.share.web.service;

import com.percussion.share.validation.PSErrors;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Maps Exceptions to a {@link PSErrors} serializable error object.
 *
 * <p>Sets an explicit media type so JAX-RS does not try to write {@link PSErrors} as {@code
 * text/html} without a writer (Preview {@code GET …/pagemanagement/render/page/{id}} — #3809).
 *
 * <p>Sunny Sal says: "Exception mapping - because even bugs need a paper trail!"
 *
 * @param <T> exception type
 */
@Provider
public abstract class PSAbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

  /** Request Accept headers; may be {@code null} in unit tests. */
  @Context HttpHeaders headers;

  /**
   * CXF injects {@link #headers}; tests in other packages set Accept via this method.
   *
   * @param headers may be {@code null}
   */
  public void setHeaders(HttpHeaders headers) {
    this.headers = headers;
  }

  @Override
  public Response toResponse(T exception) {
    var status = getStatus(exception);
    var errors = createErrors(exception);
    MediaType type = negotiateMediaType(headers);
    // HTML Preview has no Jackson writer for PSErrors (#3809). Emit a String so
    // the built-in text/html writer is used even if the HTML PSErrors provider
    // is not listed on this JAX-RS bus.
    if (MediaType.TEXT_HTML_TYPE.equals(type) || MediaType.TEXT_PLAIN_TYPE.equals(type)) {
      return Response.status(status)
          .type(type)
          .entity(PSErrorsHtmlMessageBodyWriter.toHtml(errors))
          .build();
    }
    return Response.status(status).entity(errors).type(type).build();
  }

  /**
   * HTML Preview (browser Accept {@code text/html}) uses {@link PSErrorsHtmlMessageBodyWriter}. JSON
   * API clients keep {@code application/json}.
   *
   * @param requestHeaders may be {@code null}
   * @return never {@code null}
   */
  static MediaType negotiateMediaType(HttpHeaders requestHeaders) {
    if (requestHeaders == null) {
      return MediaType.APPLICATION_JSON_TYPE;
    }
    List<MediaType> accepts = requestHeaders.getAcceptableMediaTypes();
    if (accepts == null || accepts.isEmpty()) {
      return MediaType.APPLICATION_JSON_TYPE;
    }
    boolean json = false;
    boolean xml = false;
    boolean html = false;
    for (MediaType accept : accepts) {
      if (accept == null || (accept.isWildcardType() && accept.isWildcardSubtype())) {
        continue;
      }
      if (MediaType.APPLICATION_JSON_TYPE.isCompatible(accept)) {
        json = true;
      } else if (MediaType.TEXT_HTML_TYPE.isCompatible(accept)
          || MediaType.APPLICATION_XHTML_XML_TYPE.isCompatible(accept)) {
        html = true;
      } else if (MediaType.APPLICATION_XML_TYPE.isCompatible(accept)
          || MediaType.TEXT_XML_TYPE.isCompatible(accept)) {
        xml = true;
      }
    }
    if (json) {
      return MediaType.APPLICATION_JSON_TYPE;
    }
    if (html) {
      return MediaType.TEXT_HTML_TYPE;
    }
    if (xml) {
      return MediaType.APPLICATION_XML_TYPE;
    }
    return MediaType.APPLICATION_JSON_TYPE;
  }

  /** Returns the HTTP status for the given exception. Override to provide custom status codes. */
  protected Status getStatus(T exception) {
    return Status.INTERNAL_SERVER_ERROR;
  }

  /**
   * Creates a serializable errors object from the given exception.
   *
   * @param exception never {@code null}
   * @return never {@code null}
   */
  protected abstract PSErrors createErrors(T exception);
}
