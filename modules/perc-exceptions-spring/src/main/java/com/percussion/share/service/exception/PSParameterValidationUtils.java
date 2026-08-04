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
package com.percussion.share.service.exception;

import com.percussion.share.validation.PSValidationErrors;
import com.percussion.share.validation.PSValidationErrorsBuilder;

/**
 * Utility helpers that build and throw {@link PSParametersValidationException} instances when
 * method parameters fail validation.
 *
 * @author adamgent
 */
public class PSParameterValidationUtils {

  /**
   * Default constructor required for proxy-based instantiation frameworks; this is a static utility
   * class and should not be instantiated.
   */
  public PSParameterValidationUtils() {
    super();
  }

  /**
   * Rejects the given value when it is {@code null}, throwing a {@link PSValidationException}
   * annotated with the supplied method and field names.
   *
   * @param method the name of the method being validated, never {@code null}.
   * @param field the name of the parameter being validated, never {@code null}.
   * @param value the value to check, may be {@code null}.
   * @throws PSValidationException if {@code value} is {@code null}.
   */
  public static void rejectIfNull(String method, String field, Object value)
      throws PSValidationException {
    throwIfErrors(new PSValidationErrorsBuilder(method).rejectIfNull(field, value).build());
  }

  /**
   * Rejects the given value when it is {@code null}, empty, or contains only whitespace, throwing a
   * {@link PSValidationException} annotated with the supplied method and field names.
   *
   * @param method the name of the method being validated, never {@code null}.
   * @param field the name of the parameter being validated, never {@code null}.
   * @param value the value to check, may be {@code null}.
   * @throws PSValidationException if {@code value} is {@code null}, empty, or blank.
   */
  public static void rejectIfBlank(String method, String field, String value)
      throws PSValidationException {
    throwIfErrors(new PSValidationErrorsBuilder(method).rejectIfBlank(field, value).build());
  }

  /**
   * Creates a new {@link PSValidationErrorsBuilder} associated with the given method name so
   * callers can chain additional rejection rules.
   *
   * @param method the name of the method being validated, never {@code null}.
   * @return a new builder, never {@code null}.
   */
  public static PSValidationErrorsBuilder validateParameters(String method) {
    return new PSValidationErrorsBuilder(method);
  }

  /**
   * Throws a {@link PSParametersValidationException} when the supplied validation errors contain at
   * least one entry.
   *
   * @param pve the validation errors to evaluate, never {@code null}.
   * @throws PSValidationException if the validation errors contain any entries.
   */
  public static void throwIfErrors(PSValidationErrors pve) throws PSValidationException {
    new PSParametersValidationException(pve).throwIfInvalid();
  }
}
