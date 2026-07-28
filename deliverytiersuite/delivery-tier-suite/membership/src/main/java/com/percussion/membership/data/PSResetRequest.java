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
 * Carries the data required to request a member password reset.
 *
 * @author Percussion Software
 */
public class PSResetRequest {
  /** The email of the member requesting the reset. */
  private String email;

  /** The page to redirect the user to once the reset request completes. */
  private String redirectPage;

  /** Default constructor for use by serialization frameworks. */
  public PSResetRequest() {}

  /**
   * Gets the email of the member requesting the reset.
   *
   * @return the email address, may be {@code null} if not set.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email of the member requesting the reset.
   *
   * @param email the email address, may not be {@code null}.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the page to redirect the user to once the reset request completes.
   *
   * @return the redirect page, may be {@code null} if not set.
   */
  public String getRedirectPage() {
    return redirectPage;
  }

  /**
   * Sets the page to redirect the user to once the reset request completes.
   *
   * @param redirectPage the redirect page, may not be {@code null}.
   */
  public void setRedirectPage(String redirectPage) {
    this.redirectPage = redirectPage;
  }
}
