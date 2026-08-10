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
package com.percussion.sitemanage.error;

import com.percussion.share.service.exception.PSDataServiceException;

/** Exception thrown when a template import operation fails. */
public class PSTemplateImportException extends PSDataServiceException {
  private static final long serialVersionUID = 1L;

  /** Constructs a new exception with no detail message. */
  public PSTemplateImportException() {
    super();
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message.
   */
  public PSTemplateImportException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message.
   * @param cause the cause.
   */
  public PSTemplateImportException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified cause.
   *
   * @param cause the cause.
   */
  public PSTemplateImportException(Throwable cause) {
    super(cause);
  }
}
