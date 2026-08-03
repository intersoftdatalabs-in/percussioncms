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
package com.percussion.deployer.services;

/** Handles deploy service exceptions. */
public class PSDeployServiceException extends Exception {
  private static final long serialVersionUID = 1L;

  /** Default constructor for serialization frameworks. */
  public PSDeployServiceException() {
    super();
  }

  /**
   * Constructs a new exception with the supplied message and cause.
   *
   * @param message the detail message, may be <code>null</code>.
   * @param cause the cause, may be <code>null</code>.
   */
  public PSDeployServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the supplied message.
   *
   * @param message the detail message, may be <code>null</code>.
   */
  public PSDeployServiceException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the supplied cause.
   *
   * @param cause the cause, may be <code>null</code>.
   */
  public PSDeployServiceException(Throwable cause) {
    super(cause);
  }
}
