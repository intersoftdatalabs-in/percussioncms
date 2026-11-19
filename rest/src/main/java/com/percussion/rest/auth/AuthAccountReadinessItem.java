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
import java.util.ArrayList;
import java.util.List;

/** Account-level readiness item describing email hygiene and MFA factor coverage. */
@XmlRootElement(name = "AuthAccountReadinessItem")
@JsonRootName("AuthAccountReadinessItem")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Per-account email and MFA readiness details")
public class AuthAccountReadinessItem {

  private String userName;
  private String providerType;
  private String email;
  private boolean emailVerified;
  private String mfaEnrollmentState;
  private int totpCount;
  private int passkeyCount;
  private int recoveryCodeCount;
  private List<String> issues = new ArrayList<>();

  public AuthAccountReadinessItem() {}

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
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

  public String getMfaEnrollmentState() {
    return mfaEnrollmentState;
  }

  public void setMfaEnrollmentState(String mfaEnrollmentState) {
    this.mfaEnrollmentState = mfaEnrollmentState;
  }

  public int getTotpCount() {
    return totpCount;
  }

  public void setTotpCount(int totpCount) {
    this.totpCount = totpCount;
  }

  public int getPasskeyCount() {
    return passkeyCount;
  }

  public void setPasskeyCount(int passkeyCount) {
    this.passkeyCount = passkeyCount;
  }

  public int getRecoveryCodeCount() {
    return recoveryCodeCount;
  }

  public void setRecoveryCodeCount(int recoveryCodeCount) {
    this.recoveryCodeCount = recoveryCodeCount;
  }

  public List<String> getIssues() {
    return issues;
  }

  public void setIssues(List<String> issues) {
    this.issues = issues != null ? issues : new ArrayList<>();
  }
}
