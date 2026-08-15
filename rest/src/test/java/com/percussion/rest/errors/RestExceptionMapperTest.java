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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.rest.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Maps {@link RestExceptionBase} to {@link RestError} with nullable {@code errorData} (issue
 * #3430).
 */
class RestExceptionMapperTest {

  private RestExceptionMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new RestExceptionMapper();
  }

  @Test
  void mapsErrorDataAsScalarNotOptional() {
    RestExceptionBase ex =
        new RestExceptionBase(
            RestErrorCode.FOLDER_NOT_FOUND,
            "Folder missing",
            "path=/a",
            "folder-id-9",
            Response.Status.NOT_FOUND);

    Response response = mapper.toResponse(ex);

    assertEquals(404, response.getStatus());
    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertEquals(RestErrorCode.FOLDER_NOT_FOUND.getNumVal(), body.getErrorCode());
    assertEquals("Folder missing", body.getMessage());
    assertEquals("path=/a", body.getDetailMessage());
    assertEquals("folder-id-9", body.getErrorData());
  }

  @Test
  void mapsNullErrorDataAsNull() {
    RestExceptionBase ex =
        new RestExceptionBase(
            RestErrorCode.OTHER, "boom", null, null, Response.Status.INTERNAL_SERVER_ERROR);

    Response response = mapper.toResponse(ex);

    RestError body = assertInstanceOf(RestError.class, response.getEntity());
    assertNull(body.getErrorData());
    assertEquals("boom", body.getMessage());
  }
}
