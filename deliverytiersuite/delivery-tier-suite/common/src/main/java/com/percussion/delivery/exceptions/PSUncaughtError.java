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

package com.percussion.delivery.exceptions;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.utils.PSRedirectValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Provider
public class PSUncaughtError extends Throwable implements ExceptionMapper<Throwable> {
  private static final long serialVersionUID = 1L;

  private static final Logger log = LogManager.getLogger(PSUncaughtError.class);

  @Context private HttpServletRequest request;

  @Context private HttpServletResponse response;

  @Override
  public Response toResponse(Throwable exception) {
    try {
      if (request != null & response != null) {
        log.warn(
            "Page redirecting to error {} : . Error code {} : . Error message {} : ",
            request.getHeader("referer"),
            response.getStatus(),
            exception.getLocalizedMessage());
        // CWE-601 / java/unvalidated-url-redirection #1081: never build the error redirect
        // from the Referer header (open-redirect vector). Always use a local relative path.
        String context = request.getContextPath();
        if (context == null) {
          context = "";
        }
        String errorPath = context + "/error.html";
        String safe = PSRedirectValidation.validateInternalRedirectUrl(errorPath);
        if (safe == null) {
          safe = "/error.html";
        }
        response.sendRedirect(safe);
      } else {
        logErrorMessage(exception);
      }
    } catch (Exception e) {
      logErrorMessage(exception);
    }
    return null;
  }

  private void logErrorMessage(Throwable exception) {
    String errorMessage = exception.getMessage();
    if (exception instanceof Exception) {
      errorMessage = PSExceptionUtils.getMessageForLog((Exception) exception);
      log.debug(PSExceptionUtils.getDebugMessageForLog((Exception) exception));
    } else {
      log.debug(exception);
    }
    log.error(
        "Exception occurred while redirecting to error.html, Original Error is : {}", errorMessage);
  }
}
