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
package com.percussion.membership.data;

/**
 * Represents a member login request, carrying the email and password supplied by the caller.
 *
 * @author Percussion Software
 */
public class PSLoginRequest {
  private String email;
  private String password;

  /** Default constructor for use by serialization frameworks. */
  public PSLoginRequest() {}

  /**
   * Gets the email submitted by the caller.
   *
   * @return the email address, may be {@code null} if not set.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email submitted by the caller.
   *
   * @param email the email address, may not be {@code null}.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the password submitted by the caller.
   *
   * @return the password, may be {@code null} if not set.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password submitted by the caller.
   *
   * @param password the password, may not be {@code null}.
   */
  public void setPassword(String password) {
    this.password = password;
  }
}
