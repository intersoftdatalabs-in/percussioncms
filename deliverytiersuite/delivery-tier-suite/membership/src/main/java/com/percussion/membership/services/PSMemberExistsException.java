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
package com.percussion.membership.services;

/**
 * Thrown when an attempt is made to create a user account that already exists.
 *
 * @author Percussion Software
 */
public class PSMemberExistsException extends Exception {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception referencing the user that already exists.
   *
   * @param userId the user id that already exists, may not be {@code null}.
   */
  public PSMemberExistsException(String userId) {
    super("User already exists: " + userId);
  }
}
