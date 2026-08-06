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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebApplicationExceptionMapper} (REST-ERR-01 / REST-ERR-02).
 *
 * <p>Covers status preservation, RestError entity shape, and cause-preserving client messages.
 */
class WebApplicationExceptionMapperTest {

  private WebApplicationExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new WebApplicationExceptionMapper();
  }

  @Test
  void mapsMessageAndStatusToRestError() {
    WebApplicationException ex = new WebApplicationException("Control not found", 404);

    Response response = mapper.toResponse(ex);

    assertEquals(404, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals(RestErrorCode.OTHER.getNumVal(), body.getErrorCode());
    assertEquals("WebApplicationException", body.getErrorType());
    assertEquals("Control not found", body.getMessage());
  }

  @Test
  void preservesCauseMessageForStatusOnlyConstructor() {
    RuntimeException cause = new IllegalStateException("adaptor boom");
    // Catalog pattern: throw new WebApplicationException(e, 500)
    WebApplicationException ex = new WebApplicationException(cause, 500);

    Response response = mapper.toResponse(ex);

    assertEquals(500, response.getStatus());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals("adaptor boom", body.getMessage());
    assertNotNull(body.getDetailMessage());
    assertTrue(
        body.getDetailMessage().contains("IllegalStateException")
            || body.getDetailMessage().contains("adaptor boom"),
        "detail should mention cause type or message: " + body.getDetailMessage());
  }

  @Test
  void nestedCauseUsesDeepestMessage() {
    Throwable root = new RuntimeException("root failure");
    Throwable mid = new IllegalArgumentException("mid", root);
    WebApplicationException ex = new WebApplicationException(mid, 500);

    Response response = mapper.toResponse(ex);

    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals("root failure", body.getMessage());
  }

  @Test
  void stringEntityBecomesMessage() {
    WebApplicationException ex =
        new WebApplicationException(
            Response.status(400).entity("No file sent").type(MediaType.TEXT_PLAIN).build());

    Response response = mapper.toResponse(ex);

    assertEquals(400, response.getStatus());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals("No file sent", body.getMessage());
  }

  @Test
  void existingRestErrorEntityIsPreserved() {
    RestError existing =
        new RestError(
            RestErrorCode.NOT_AUTHORIZED.getNumVal(),
            "NotAuthorizedException",
            "denied",
            "detail",
            null);
    WebApplicationException ex =
        new WebApplicationException(Response.status(403).entity(existing).build());

    Response response = mapper.toResponse(ex);

    assertEquals(403, response.getStatus());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals(RestErrorCode.NOT_AUTHORIZED.getNumVal(), body.getErrorCode());
    assertEquals("denied", body.getMessage());
    assertEquals("detail", body.getDetailMessage());
  }

  @Test
  void restExceptionBaseSubtypeUsesRestExceptionFields() {
    RestExceptionBase ex =
        new RestExceptionBase(
            RestErrorCode.FOLDER_NOT_FOUND,
            "Folder missing",
            "path=/a",
            null,
            Response.Status.NOT_FOUND);

    Response response = mapper.toResponse(ex);

    assertEquals(404, response.getStatus());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals(RestErrorCode.FOLDER_NOT_FOUND.getNumVal(), body.getErrorCode());
    assertEquals("RestExceptionBase", body.getErrorType());
    assertEquals("Folder missing", body.getMessage());
    assertEquals("path=/a", body.getDetailMessage());
  }

  @Test
  void genericHttpMessageHelpers() {
    assertTrue(WebApplicationExceptionMapper.isGenericHttpStatusMessage(null));
    assertTrue(
        WebApplicationExceptionMapper.isGenericHttpStatusMessage("HTTP 500 Internal Server Error"));
    assertTrue(WebApplicationExceptionMapper.isGenericHttpStatusMessage("http 404 Not Found"));
  }
}
