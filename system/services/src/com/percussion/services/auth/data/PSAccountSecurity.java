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
package com.percussion.services.auth.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/** JPA entity for {@code PSX_ACCOUNT_SECURITY}. */
@Entity
@Table(name = "PSX_ACCOUNT_SECURITY")
public class PSAccountSecurity implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String MFA_STATE_NONE = "NONE";
  public static final String MFA_STATE_OPTIONAL = "OPTIONAL";
  public static final String MFA_STATE_REQUIRED = "REQUIRED";
  public static final String MFA_STATE_ENROLLED = "ENROLLED";
  public static final String MFA_STATE_EXEMPT = "EXEMPT";

  public static final String POLICY_INTERACTIVE_MFA_REQUIRED = "INTERACTIVE_MFA_REQUIRED";
  public static final String POLICY_NON_INTERACTIVE_SERVICE = "NON_INTERACTIVE_SERVICE";
  public static final String POLICY_SSO_EXTERNAL_MFA = "SSO_EXTERNAL_MFA";

  @Id
  @Column(name = "USER_NAME", nullable = false, length = 255)
  private String userName;

  @Column(name = "EMAIL_NORMALIZED", length = 320)
  private String emailNormalized;

  @Column(name = "EMAIL_VERIFIED", nullable = false, length = 1)
  private String emailVerified = "N";

  @Column(name = "EMAIL_VERIFIED_AT")
  private Date emailVerifiedAt;

  @Column(name = "PENDING_EMAIL", length = 320)
  private String pendingEmail;

  @Column(name = "MFA_ENROLLMENT_STATE", nullable = false, length = 32)
  private String mfaEnrollmentState = MFA_STATE_NONE;

  @Column(name = "ACCOUNT_POLICY", nullable = false, length = 32)
  private String accountPolicy = POLICY_INTERACTIVE_MFA_REQUIRED;

  @Column(name = "MFA_ENFORCEMENT_COHORT", length = 64)
  private String mfaEnforcementCohort;

  @Column(name = "SESSION_VERSION", nullable = false)
  private long sessionVersion = 1L;

  @Column(name = "MFA_ENFORCED_AT")
  private Date mfaEnforcedAt;

  @Column(name = "PASSWORD_CHANGED_AT")
  private Date passwordChangedAt;

  @Column(name = "FAILED_LOGIN_COUNT", nullable = false)
  private int failedLoginCount;

  @Column(name = "LOCKED_UNTIL")
  private Date lockedUntil;

  @Column(name = "RECOVERY_LOCKED_UNTIL")
  private Date recoveryLockedUntil;

  @Column(name = "UPDATED_AT", nullable = false)
  private Date updatedAt = new Date();

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public void setEmailNormalized(String emailNormalized) {
    this.emailNormalized = emailNormalized;
  }

  public boolean isEmailVerified() {
    return "Y".equalsIgnoreCase(emailVerified);
  }

  public void setEmailVerified(boolean verified) {
    this.emailVerified = verified ? "Y" : "N";
  }

  public Date getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(Date emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public String getPendingEmail() {
    return pendingEmail;
  }

  public void setPendingEmail(String pendingEmail) {
    this.pendingEmail = pendingEmail;
  }

  public String getMfaEnrollmentState() {
    return mfaEnrollmentState;
  }

  public void setMfaEnrollmentState(String mfaEnrollmentState) {
    this.mfaEnrollmentState = mfaEnrollmentState != null ? mfaEnrollmentState : MFA_STATE_NONE;
  }

  public String getAccountPolicy() {
    return accountPolicy;
  }

  public void setAccountPolicy(String accountPolicy) {
    this.accountPolicy =
        accountPolicy != null ? accountPolicy : POLICY_INTERACTIVE_MFA_REQUIRED;
  }

  public String getMfaEnforcementCohort() {
    return mfaEnforcementCohort;
  }

  public void setMfaEnforcementCohort(String mfaEnforcementCohort) {
    this.mfaEnforcementCohort = mfaEnforcementCohort;
  }

  public long getSessionVersion() {
    return sessionVersion;
  }

  public void setSessionVersion(long sessionVersion) {
    this.sessionVersion = sessionVersion;
  }

  public Date getMfaEnforcedAt() {
    return mfaEnforcedAt;
  }

  public void setMfaEnforcedAt(Date mfaEnforcedAt) {
    this.mfaEnforcedAt = mfaEnforcedAt;
  }

  public Date getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public void setPasswordChangedAt(Date passwordChangedAt) {
    this.passwordChangedAt = passwordChangedAt;
  }

  public int getFailedLoginCount() {
    return failedLoginCount;
  }

  public void setFailedLoginCount(int failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public Date getLockedUntil() {
    return lockedUntil;
  }

  public void setLockedUntil(Date lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  public Date getRecoveryLockedUntil() {
    return recoveryLockedUntil;
  }

  public void setRecoveryLockedUntil(Date recoveryLockedUntil) {
    this.recoveryLockedUntil = recoveryLockedUntil;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt != null ? updatedAt : new Date();
  }
}
