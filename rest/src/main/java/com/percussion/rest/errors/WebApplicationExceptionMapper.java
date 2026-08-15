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

package com.percussion.rest.errors;

import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps bare {@link WebApplicationException} throws to typed JSON/XML {@link RestError} bodies.
 *
 * <p>{@link RestExceptionMapper} remains the more-specific mapper for {@link RestExceptionBase}.
 * This peer covers catalog resources that still throw {@code new WebApplicationException(cause,
 * status)} or message+status WAEs without a {@link RestError} entity (REST-ERR-01 / REST-ERR-02).
 *
 * <p>Client-visible {@link RestError#getMessage()} prefers a non-generic exception message, then
 * the deepest cause message (so {@code new WebApplicationException(e, 500)} surfaces {@code
 * e.getMessage()} instead of only {@code "HTTP 500 Internal Server Error"}).
 */
@Provider
@PSSiteManageBean("webApplicationExceptionMapper")
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

  @Context private HttpHeaders headers;

  @Override
  public Response toResponse(WebApplicationException e) {
    // Prefer the dedicated RestExceptionBase mapper shape when subtype slips through.
    if (e instanceof RestExceptionBase restEx) {
      return buildRestExceptionResponse(restEx);
    }

    Response existing = e.getResponse();
    int status =
        existing != null
            ? existing.getStatus()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    Object entity = existing != null ? existing.getEntity() : null;
    if (entity instanceof RestError restError) {
      return responseWithEntity(status, restError);
    }

    String message = resolveClientMessage(e, entity);
    String detailMessage = resolveDetailMessage(e, message);

    RestError restError =
        new RestError(
            RestErrorCode.OTHER.getNumVal(),
            e.getClass().getSimpleName(),
            message,
            detailMessage,
            null);

    return responseWithEntity(status, restError);
  }

  private Response buildRestExceptionResponse(RestExceptionBase e) {
    RestError restError =
        new RestError(
            e.getErrorCode() != null
                ? e.getErrorCode().getNumVal()
                : RestErrorCode.OTHER.getNumVal(),
            e.getClass().getSimpleName(),
            e.getMessage(),
            e.getDetailMessage(),
            e.getErrorData());
    int status =
        e.getStatus() != null
            ? e.getStatus().getStatusCode()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    return responseWithEntity(status, restError);
  }

  private Response responseWithEntity(int status, RestError restError) {
    Response.ResponseBuilder rb =
        Response.status(status).entity(restError).type(resolveMediaType());
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

  /**
   * Prefer an explicit WAE message or String response entity; otherwise the deepest non-blank cause
   * message (REST-ERR-02). Generic {@code "HTTP NNN ..."} phrases from JAX-RS are not treated as
   * client-visible content when a cause message exists.
   */
  static String resolveClientMessage(WebApplicationException e, Object entity) {
    if (entity instanceof String s && !s.isBlank()) {
      return s.trim();
    }

    String msg = e.getMessage();
    Throwable cause = e.getCause();
    String causeMsg = deepestMessage(cause);

    if (!isGenericHttpStatusMessage(msg) && msg != null && !msg.isBlank()) {
      return msg.trim();
    }
    if (causeMsg != null && !causeMsg.isBlank()) {
      return causeMsg.trim();
    }
    if (msg != null && !msg.isBlank()) {
      return msg.trim();
    }

    Response r = e.getResponse();
    if (r != null) {
      Response.Status st = Response.Status.fromStatusCode(r.getStatus());
      if (st != null && st.getReasonPhrase() != null && !st.getReasonPhrase().isBlank()) {
        return st.getReasonPhrase();
      }
    }
    return "Request failed";
  }

  /**
   * Secondary detail for SPA / logs: cause message when the primary message came from the WAE
   * itself, or the cause type when the primary already used the cause text.
   */
  static String resolveDetailMessage(WebApplicationException e, String clientMessage) {
    Throwable cause = e.getCause();
    if (cause == null) {
      return null;
    }
    String causeMsg = deepestMessage(cause);
    if (causeMsg != null
        && !causeMsg.isBlank()
        && clientMessage != null
        && !clientMessage.equals(causeMsg.trim())) {
      return causeMsg.trim();
    }
    String type = cause.getClass().getSimpleName();
    if (causeMsg != null && !causeMsg.isBlank()) {
      return type + ": " + causeMsg.trim();
    }
    return type;
  }

  static String deepestMessage(Throwable t) {
    if (t == null) {
      return null;
    }
    Throwable cur = t;
    String last = null;
    // Cap depth to avoid cycles / pathological chains.
    for (int i = 0; i < 16 && cur != null; i++) {
      String m = cur.getMessage();
      if (m != null && !m.isBlank()) {
        last = m;
      }
      Throwable next = cur.getCause();
      if (next == null || next == cur) {
        break;
      }
      cur = next;
    }
    return last;
  }

  static boolean isGenericHttpStatusMessage(String msg) {
    if (msg == null) {
      return true;
    }
    String trimmed = msg.trim();
    // JAX-RS WebApplicationException uses "HTTP {code} {reason}" when built from status/cause.
    return trimmed.regionMatches(true, 0, "HTTP ", 0, 5);
  }
}
