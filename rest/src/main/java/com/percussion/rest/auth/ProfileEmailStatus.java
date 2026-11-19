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
import java.time.Instant;

/** Current account email verification state for the authenticated session user. */
@XmlRootElement(name = "ProfileEmailStatus")
@JsonRootName("ProfileEmailStatus")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authenticated user's email verification and pending-change status")
public class ProfileEmailStatus {

  private String userName;
  private String email;
  private boolean emailVerified;
  private Instant emailVerifiedAt;
  private String pendingEmail;
  private boolean emailEditable;
  private boolean emailRequired;
  private String providerType;

  public ProfileEmailStatus() {}

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(Instant emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public String getPendingEmail() {
    return pendingEmail;
  }

  public void setPendingEmail(String pendingEmail) {
    this.pendingEmail = pendingEmail;
  }

  public boolean isEmailEditable() {
    return emailEditable;
  }

  public void setEmailEditable(boolean emailEditable) {
    this.emailEditable = emailEditable;
  }

  public boolean isEmailRequired() {
    return emailRequired;
  }

  public void setEmailRequired(boolean emailRequired) {
    this.emailRequired = emailRequired;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }
}
