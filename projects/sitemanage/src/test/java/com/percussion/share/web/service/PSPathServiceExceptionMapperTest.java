/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.service.exception.IPSNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-867 / v8.1.7 PR #924: path-not-found maps to HTTP 404, not 500.
 *
 * <p>Folder-create race retries already live in {@code PSPathItemService.addFolder}; this covers
 * the exception mapper status mapping.
 */
class PSPathServiceExceptionMapperTest {

  private final PSPathServiceExceptionMapper mapper = new PSPathServiceExceptionMapper();

  @Test
  void pathNotFoundServiceExceptionIsNotFound() {
    IPSPathService.PSPathNotFoundServiceException ex =
        new IPSPathService.PSPathNotFoundServiceException("Path not found");
    assertEquals(Response.Status.NOT_FOUND, mapper.getStatus(ex));
  }

  private static class CustomNotFoundException extends Exception implements IPSNotFoundException {
    private static final long serialVersionUID = 1L;
  }

  @Test
  void notFoundCauseIsNotFound() {
    IPSPathService.PSPathServiceException ex =
        new IPSPathService.PSPathServiceException("Wrapper", new CustomNotFoundException());
    assertEquals(Response.Status.NOT_FOUND, mapper.getStatus(ex));
  }

  @Test
  void nestedNotFoundCauseIsNotFound() {
    Exception mid = new RuntimeException("mid", new CustomNotFoundException());
    IPSPathService.PSPathServiceException ex =
        new IPSPathService.PSPathServiceException("outer wrap", mid);
    assertEquals(Response.Status.NOT_FOUND, mapper.getStatus(ex));
  }

  @Test
  void genericPathServiceExceptionIsServerError() {
    IPSPathService.PSPathServiceException ex =
        new IPSPathService.PSPathServiceException("Generic error");
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR, mapper.getStatus(ex));
  }
}
