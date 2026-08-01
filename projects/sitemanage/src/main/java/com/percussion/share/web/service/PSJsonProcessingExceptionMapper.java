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

import com.percussion.share.service.exception.PSErrorUtils;
import com.percussion.share.validation.PSErrors;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

/**
 * Maps {@link JacksonException} to a serializable error object. Sunny Sal says: "JSON parsing
 * failed? Let's keep it classy and JSON-y!"
 */
@Provider
@Component
@Produces(MediaType.APPLICATION_JSON)
public class PSJsonProcessingExceptionMapper extends PSAbstractExceptionMapper<JacksonException>
    implements ExceptionMapper<JacksonException> {

  private static final String ERROR_MESSAGE = "JSON error: ";

  /** The log instance to use for this class, never {@code null}. */
  private static final Logger log = LogManager.getLogger(JacksonException.class);

  @Override
  protected PSErrors createErrors(JacksonException exception) {
    var errorMessage = exception.getClass().getName();
    if (log.isDebugEnabled()) {
      log.debug(errorMessage, exception);
    }
    return PSErrorUtils.createErrorsFromException(exception);
  }
}
