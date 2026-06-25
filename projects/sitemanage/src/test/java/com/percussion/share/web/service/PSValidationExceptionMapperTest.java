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

package com.percussion.share.web.service;

import static org.junit.Assert.assertEquals;

import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import javax.ws.rs.core.Response;
import org.junit.Test;

public class PSValidationExceptionMapperTest {

  private final PSValidationExceptionMapper validationMapper = new PSValidationExceptionMapper();

  private final PSBeanValidationExceptionMapper beanValidationMapper =
      new PSBeanValidationExceptionMapper();

  private final PSSpringValidationExceptionMapper springValidationMapper =
      new PSSpringValidationExceptionMapper();

  private static class DummyValidationException extends PSValidationException {
    private static final long serialVersionUID = 1L;

    public DummyValidationException(String message) {
      super(message);
    }
  }

  private static class DummySpringValidationException extends PSSpringValidationException {
    private static final long serialVersionUID = 1L;

    public DummySpringValidationException(String message) {
      super(message);
    }
  }

  @Test
  public void testValidationExceptionMapper_getStatus() {
    DummyValidationException ex = new DummyValidationException("Invalid input");
    assertEquals(Response.Status.BAD_REQUEST, validationMapper.getStatus(ex));
  }

  @Test
  public void testBeanValidationExceptionMapper_getStatus() {
    Object target = new Object();
    PSBeanValidationException ex = new PSBeanValidationException(target, "testMethod");
    assertEquals(Response.Status.BAD_REQUEST, beanValidationMapper.getStatus(ex));
  }

  @Test
  public void testSpringValidationExceptionMapper_getStatus() {
    DummySpringValidationException ex =
        new DummySpringValidationException("Spring validation failed");
    assertEquals(Response.Status.BAD_REQUEST, springValidationMapper.getStatus(ex));
  }
}
