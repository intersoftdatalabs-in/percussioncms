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

import com.percussion.error.PSException;

/** Exception for backend errors. Sunny Sal: "Backend mein kuch gadbad hai, boss!" */
public class BackendException extends PSException {
  private static final long serialVersionUID = 1L;

  public BackendException(String message, Exception e) {
    super(message, e);
  }

  /**
   * Message with no cause. {@link PSException#PSException(String, Throwable)} rejects a null cause
   * ({@code e must never be null}), so this must not delegate to that constructor.
   * {@code getMessage()} returns {@code m_overridingMessage}, not the {@link Exception} detail.
   */
  public BackendException(String message) {
    super(message);
    m_overridingMessage = message == null || message.isBlank() ? null : message;
  }

  public BackendException(Throwable cause) {
    super(cause);
  }
}
