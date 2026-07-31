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

/**
 * Signals that an error occurred while sending or preparing an email message in the delivery tier.
 */
public class PSEmailException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new email exception that wraps the supplied cause.
   *
   * @param e the underlying cause of the failure, never <code>null</code>.
   */
  public PSEmailException(Exception e) {
    super(e);
  }

  /**
   * Constructs a new email exception with the supplied message.
   *
   * @param s the message describing the failure, may be <code>null</code>.
   */
  public PSEmailException(String s) {
    super(s);
  }
}
