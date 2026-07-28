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
 * Carries the data required to create a new membership account.
 *
 * @author nischalraghavendra
 */
public class PSMembershipAccount {
  /** The email for the account to create. Never <code>null</code> or empty. */
  private String email;

  /** The password for the account to create. Never <code>null</code> or empty. */
  private String password;

  /** Indicates if the confirmation is required. Never <code>null</code> or empty. */
  private Boolean confirmationRequired;

  /** The confirmation page to redirect the user. Never <code>null</code> or empty. */
  private String confirmationPage;

  /** Default constructor for use by serialization frameworks. */
  public PSMembershipAccount() {}

  /**
   * Gets the email address for the account to create.
   *
   * @return the email address, never <code>null</code> or empty.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email address for the account to create.
   *
   * @param email the email address, may not be <code>null</code>.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Gets the password for the account to create.
   *
   * @return the password, never <code>null</code> or empty.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password used to activate the account.
   *
   * @param password the password to set, may not be <code>null</code>.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Indicates whether the account requires a confirmation step before activation.
   *
   * @return {@code true} when confirmation is required, {@code false} otherwise.
   */
  public Boolean isConfirmationRequired() {
    return confirmationRequired == null ? Boolean.FALSE : confirmationRequired;
  }

  /**
   * Sets whether the account requires a confirmation step before activation.
   *
   * @param confirmationRequired {@code true} when confirmation is required, {@code false}
   *     otherwise.
   */
  public void setConfirmationRequired(Boolean confirmationRequired) {
    this.confirmationRequired = confirmationRequired;
  }

  /**
   * Gets the confirmation page to redirect the user to once the account is confirmed.
   *
   * @return the confirmation page, never <code>null</code> or empty.
   */
  public String getConfirmationPage() {
    return confirmationPage;
  }

  /**
   * Sets the confirmation page to redirect the user to once the account is confirmed.
   *
   * @param confirmationPage the confirmation page, may not be <code>null</code>.
   */
  public void setConfirmationPage(String confirmationPage) {
    this.confirmationPage = confirmationPage;
  }
}
