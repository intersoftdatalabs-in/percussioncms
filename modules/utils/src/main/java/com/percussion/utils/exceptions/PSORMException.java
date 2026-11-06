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
package com.percussion.utils.exceptions;

/**
 * Represent problems with the data layer in object persistence
 *
 * @author dougrand
 */
public class PSORMException extends Exception {
  /** Serialization id. */
  private static final long serialVersionUID = 3256441387154159155L;

  /** Creates an exception with no message or cause. */
  public PSORMException() {
    super();
  }

  /**
   * Creates an exception with the supplied message.
   *
   * @param message the detail message, may be <code>null</code>
   */
  public PSORMException(String message) {
    super(message);
  }

  /**
   * Creates an exception with the supplied message and cause.
   *
   * @param message the detail message, may be <code>null</code>
   * @param cause the cause, may be <code>null</code>
   */
  public PSORMException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates an exception with the supplied cause.
   *
   * @param cause the cause, may be <code>null</code>
   */
  public PSORMException(Throwable cause) {
    super(cause);
  }
}
