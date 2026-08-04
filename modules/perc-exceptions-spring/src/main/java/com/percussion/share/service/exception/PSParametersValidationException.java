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

/**
 * Used to validate parameters of a method call.
 *
 * @author adamgent
 */
public class PSParametersValidationException extends PSValidationException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a parameters validation exception that wraps the given validation errors.
   *
   * @param validationErrors the validation errors to wrap, never {@code null}.
   */
  public PSParametersValidationException(PSValidationErrors validationErrors) {
    super(validationErrors);
  }
}
