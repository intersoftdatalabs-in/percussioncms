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

import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.validation.PSValidationErrors.PSFieldError;
import com.percussion.system.utils.PSSiteManageBean;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.exception.ConstraintsViolatedException;

/**
 * Maps {@link ConstraintsViolatedException} to a serializable error object. Sunny Sal says:
 * "Constraints violated? Let's keep it civil and informative."
 */
@Provider
@PSSiteManageBean("constraintsViolatedExceptionMapper")
public class PSConstraintsViolatedExceptionMapper
    extends PSAbstractExceptionMapper<ConstraintsViolatedException>
    implements ExceptionMapper<ConstraintsViolatedException> {

  @Override
  protected PSValidationErrors createErrors(ConstraintsViolatedException exception) {
    var ve = new PSValidationErrors();
    convert(ve, exception);
    return ve;
  }

  protected void convert(PSValidationErrors ve, ConstraintsViolatedException ce) {
    // TODO: Get the method name correctly through annotations or other means.
    ve.setMethodName(ce.getLocalizedMessage());
    var violations = ce.getConstraintViolations();
    Arrays.stream(violations).map(this::convert).forEach(ve.getFieldErrors()::add);
  }

  protected PSFieldError convert(ConstraintViolation cv) {
    var fieldError = new PSFieldError();
    fieldError.setCode(cv.getErrorCode());
    fieldError.setDefaultMessage(cv.getMessage());
    if (cv.getContext() instanceof MethodParameterContext context) {
      var paramAnnotations = context.getMethod().getParameterAnnotations();
      var annotations = paramAnnotations[context.getParameterIndex()];
      Arrays.stream(annotations)
          .filter(anot -> anot.annotationType() == PathParam.class)
          .findFirst()
          .ifPresent(anot -> fieldError.setField(((PathParam) anot).value()));
    }
    fieldError.setRejectedValue(cv.getInvalidValue());
    return fieldError;
  }
}
