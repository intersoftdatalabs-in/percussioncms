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

/** JPA entity for {@code PSX_MFA_TOTP}. Never expose {@code secretEnc} in logs or {@code toString}. */
@Entity
@Table(name = "PSX_MFA_TOTP")
public class PSMfaTotp implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_REVOKED = "REVOKED";

  @Id
  @Column(name = "TOTP_ID", nullable = false, length = 64)
  private String totpId;

  @Column(name = "USER_NAME", nullable = false, length = 255)
  private String userName;

  @Column(name = "LABEL", nullable = false, length = 128)
  private String label;

  @Column(name = "SECRET_ENC", nullable = false, length = 1024)
  private String secretEnc;

  @Column(name = "ALGORITHM", nullable = false, length = 16)
  private String algorithm = "SHA1";

  @Column(name = "DIGITS", nullable = false)
  private int digits = 6;

  @Column(name = "PERIOD_SECONDS", nullable = false)
  private int periodSeconds = 30;

  @Column(name = "STATUS", nullable = false, length = 16)
  private String status = STATUS_PENDING;

  @Column(name = "LAST_USED_STEP")
  private Long lastUsedStep;

  @Column(name = "CREATED_AT", nullable = false)
  private Date createdAt = new Date();

  @Column(name = "VERIFIED_AT")
  private Date verifiedAt;

  @Column(name = "LAST_USED_AT")
  private Date lastUsedAt;

  public String getTotpId() {
    return totpId;
  }

  public void setTotpId(String totpId) {
    this.totpId = totpId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getSecretEnc() {
    return secretEnc;
  }

  public void setSecretEnc(String secretEnc) {
    this.secretEnc = secretEnc;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public int getDigits() {
    return digits;
  }

  public void setDigits(int digits) {
    this.digits = digits;
  }

  public int getPeriodSeconds() {
    return periodSeconds;
  }

  public void setPeriodSeconds(int periodSeconds) {
    this.periodSeconds = periodSeconds;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getLastUsedStep() {
    return lastUsedStep;
  }

  public void setLastUsedStep(Long lastUsedStep) {
    this.lastUsedStep = lastUsedStep;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt != null ? createdAt : new Date();
  }

  public Date getVerifiedAt() {
    return verifiedAt;
  }

  public void setVerifiedAt(Date verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public Date getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Date lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  @Override
  public String toString() {
    return "PSMfaTotp{totpId='"
        + totpId
        + "', userName='"
        + userName
        + "', status='"
        + status
        + "', secretEnc=[REDACTED]}";
  }
}
