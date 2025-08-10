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

// REFACTORED: CP-JAVA11

package com.percussion.rest.errors;

import javax.ws.rs.core.Response;

/**
 * Exception thrown when a template is not found. Sunny Sal: "Template missing? Koi na, agla try
 * karo!"
 */
public class TemplateNotFoundException extends RestExceptionBase {

  private static final long serialVersionUID = -613886841430682824L;

  /** Constructs a TemplateNotFoundException with NOT_FOUND status. */
  public TemplateNotFoundException() {
    super(RestErrorCode.TEMPLATE_NOT_FOUND, null, null, Response.Status.NOT_FOUND);
  }

  /**
   * Constructs a TemplateNotFoundException with a detail message.
   *
   * @param detailMessage the detail message
   */
  public TemplateNotFoundException(String detailMessage) {
    super(RestErrorCode.TEMPLATE_NOT_FOUND, detailMessage, null, Response.Status.NOT_FOUND);
  }
}
