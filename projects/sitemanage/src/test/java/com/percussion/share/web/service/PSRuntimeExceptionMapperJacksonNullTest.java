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

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-675 / v8.1.7 PR #676: map Jackson/null-style runtime failures to BAD_REQUEST.
 */
class PSRuntimeExceptionMapperJacksonNullTest {

  private final PSRuntimeExceptionMapper mapper = new PSRuntimeExceptionMapper();

  private Response.Status statusOf(RuntimeException ex) throws Exception {
    Method m =
        PSRuntimeExceptionMapper.class.getDeclaredMethod("getStatus", RuntimeException.class);
    m.setAccessible(true);
    return (Response.Status) m.invoke(mapper, ex);
  }

  @Test
  void nullPointerIsBadRequest() throws Exception {
    assertEquals(Response.Status.BAD_REQUEST, statusOf(new NullPointerException("npe")));
  }

  @Test
  void illegalArgumentIsBadRequest() throws Exception {
    assertEquals(Response.Status.BAD_REQUEST, statusOf(new IllegalArgumentException("bad")));
  }

  @Test
  void jacksonCauseIsBadRequest() throws Exception {
    RuntimeException wrap =
        new RuntimeException("wrap", new JsonProcessingException("bad json") {});
    assertEquals(Response.Status.BAD_REQUEST, statusOf(wrap));
  }
}
