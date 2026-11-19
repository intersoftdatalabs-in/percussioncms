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
package com.percussion.rest.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/** Result of staging or resending self-service email verification. */
@XmlRootElement(name = "ProfileEmailActionResult")
@JsonRootName("ProfileEmailActionResult")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of staging or resending profile email verification")
public class ProfileEmailActionResult {

  private boolean verificationSent;
  private boolean appliedImmediately;
  private String email;
  private String pendingEmail;
  private boolean emailVerified;
  private String message;

  public ProfileEmailActionResult() {}

  public ProfileEmailActionResult(
      boolean verificationSent,
      boolean appliedImmediately,
      String email,
      String pendingEmail,
      boolean emailVerified,
      String message) {
    this.verificationSent = verificationSent;
    this.appliedImmediately = appliedImmediately;
    this.email = email;
    this.pendingEmail = pendingEmail;
    this.emailVerified = emailVerified;
    this.message = message;
  }

  public boolean isVerificationSent() {
    return verificationSent;
  }

  public void setVerificationSent(boolean verificationSent) {
    this.verificationSent = verificationSent;
  }

  public boolean isAppliedImmediately() {
    return appliedImmediately;
  }

  public void setAppliedImmediately(boolean appliedImmediately) {
    this.appliedImmediately = appliedImmediately;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPendingEmail() {
    return pendingEmail;
  }

  public void setPendingEmail(String pendingEmail) {
    this.pendingEmail = pendingEmail;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
