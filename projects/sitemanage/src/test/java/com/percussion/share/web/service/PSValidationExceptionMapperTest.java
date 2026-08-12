package com.percussion.share.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.share.service.exception.PSSpringValidationException;
import com.percussion.share.service.exception.PSValidationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/** Regression for GH-866 / v8.1.7 PR #929: validation exception mappers return BAD_REQUEST. */
class PSValidationExceptionMapperTest {

  private final PSValidationExceptionMapper validationMapper = new PSValidationExceptionMapper();
  private final PSBeanValidationExceptionMapper beanValidationMapper =
      new PSBeanValidationExceptionMapper();
  private final PSSpringValidationExceptionMapper springValidationMapper =
      new PSSpringValidationExceptionMapper();

  private static final class DummyValidationException extends PSValidationException {
    private static final long serialVersionUID = 1L;

    DummyValidationException(String message) {
      super(message);
    }
  }

  private static final class DummySpringValidationException extends PSSpringValidationException {
    private static final long serialVersionUID = 1L;

    DummySpringValidationException(String message) {
      super(message);
    }
  }

  @Test
  void validationExceptionMapper_getStatus() {
    assertEquals(
        Response.Status.BAD_REQUEST,
        validationMapper.getStatus(new DummyValidationException("Invalid input")));
  }

  @Test
  void beanValidationExceptionMapper_getStatus() {
    PSBeanValidationException ex = new PSBeanValidationException(new Object(), "testMethod");
    assertEquals(Response.Status.BAD_REQUEST, beanValidationMapper.getStatus(ex));
  }

  @Test
  void springValidationExceptionMapper_getStatus() {
    assertEquals(
        Response.Status.BAD_REQUEST,
        springValidationMapper.getStatus(new DummySpringValidationException("fail")));
  }
}
