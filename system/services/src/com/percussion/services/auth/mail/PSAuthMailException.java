/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.auth.mail;

/** Safe operational exception thrown when transactional auth email cannot be sent. */
public class PSAuthMailException extends Exception {

  private static final long serialVersionUID = 1L;

  public PSAuthMailException(String message) {
    super(message);
  }

  public PSAuthMailException(String message, Throwable cause) {
    super(message, cause);
  }
}
