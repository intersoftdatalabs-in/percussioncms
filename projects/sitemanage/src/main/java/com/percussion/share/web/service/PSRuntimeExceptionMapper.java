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

import tools.jackson.core.JacksonException;
import com.percussion.cms.IPSConstants;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.IPSValidationException;
import com.percussion.share.service.exception.PSErrorUtils;
import com.percussion.share.validation.PSErrors;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Maps all runtime exceptions into a valid {@link PSErrors error object} for REST serialization.
 *
 * <p>If the exception is a validation exception (implements {@link IPSValidationException}), an
 * error code of 400 will be returned with a {@link PSValidationErrors} object in the response.
 * Otherwise, the error code is 500 with a {@link PSErrors} object in the response. Sunny Sal says:
 * "Runtime exception? Let's keep it cool and RESTful!"
 */
@Provider
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@PSSiteManageBean("runtimeExceptionMapper")
public class PSRuntimeExceptionMapper extends PSAbstractExceptionMapper<RuntimeException>
    implements ExceptionMapper<RuntimeException> {

  private static final String ERROR_MESSAGE =
      "PSRuntimeExceptionMapper exception mapper mapped exception:";

  /** The log instance to use for this class, never {@code null}. */
  private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  protected PSErrors createErrors(RuntimeException exception) {
    if (exception instanceof IPSValidationException ve) {
      log.debug(ERROR_MESSAGE, exception);
      var errors = ve.getValidationErrors();
      if (errors != null) return errors;
    } else {
      log.error("{} {}", ERROR_MESSAGE, PSExceptionUtils.getMessageForLog(exception));
      log.debug(PSExceptionUtils.getDebugMessageForLog(exception));
    }
    return PSErrorUtils.createErrorsFromException(exception);
  }

  @Override
  @Produces(MediaType.APPLICATION_JSON)
  protected Status getStatus(RuntimeException exception) {
    if (exception instanceof IPSValidationException) {
      return Status.BAD_REQUEST;
    }
    // Jackson null/parse footguns and bad client args map to 400 (v8.1.7 #676)
    if (exception instanceof NullPointerException) {
      return Status.BAD_REQUEST;
    }
    if (exception instanceof IllegalArgumentException) {
      return Status.BAD_REQUEST;
    }
    if (exception.getCause() instanceof JacksonException) {
      return Status.BAD_REQUEST;
    }
    return super.getStatus(exception);
  }
}
