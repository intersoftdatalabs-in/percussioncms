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
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/** JPA entity for {@code PSX_MFA_PASSKEY}. */
@Entity
@Table(name = "PSX_MFA_PASSKEY")
public class PSMfaPasskey implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String STATUS_ACTIVE = "ACTIVE";
  public static final String STATUS_REVOKED = "REVOKED";

  @Id
  @Column(name = "PASSKEY_ID", nullable = false, length = 64)
  private String passkeyId;

  @Column(name = "USER_NAME", nullable = false, length = 255)
  private String userName;

  @Column(name = "CREDENTIAL_ID", nullable = false, length = 512)
  private String credentialId;

  @Column(name = "USER_HANDLE", nullable = false, length = 128)
  private String userHandle;

  @Lob
  @Column(name = "COSE_PUBLIC_KEY_B64", nullable = false)
  private String cosePublicKeyB64;

  @Column(name = "AAGUID", length = 64)
  private String aaguid;

  @Column(name = "SIGN_COUNT", nullable = false)
  private long signCount;

  @Column(name = "TRANSPORTS", length = 255)
  private String transports;

  @Column(name = "BACKUP_ELIGIBLE", nullable = false, length = 1)
  private String backupEligible = "N";

  @Column(name = "BACKUP_STATE", nullable = false, length = 1)
  private String backupState = "N";

  @Column(name = "LABEL", nullable = false, length = 128)
  private String label;

  @Column(name = "STATUS", nullable = false, length = 16)
  private String status = STATUS_ACTIVE;

  @Column(name = "CREATED_AT", nullable = false)
  private Date createdAt = new Date();

  @Column(name = "LAST_USED_AT")
  private Date lastUsedAt;

  public String getPasskeyId() {
    return passkeyId;
  }

  public void setPasskeyId(String passkeyId) {
    this.passkeyId = passkeyId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getCredentialId() {
    return credentialId;
  }

  public void setCredentialId(String credentialId) {
    this.credentialId = credentialId;
  }

  public String getUserHandle() {
    return userHandle;
  }

  public void setUserHandle(String userHandle) {
    this.userHandle = userHandle;
  }

  public String getCosePublicKeyB64() {
    return cosePublicKeyB64;
  }

  public void setCosePublicKeyB64(String cosePublicKeyB64) {
    this.cosePublicKeyB64 = cosePublicKeyB64;
  }

  public String getAaguid() {
    return aaguid;
  }

  public void setAaguid(String aaguid) {
    this.aaguid = aaguid;
  }

  public long getSignCount() {
    return signCount;
  }

  public void setSignCount(long signCount) {
    this.signCount = signCount;
  }

  public String getTransports() {
    return transports;
  }

  public void setTransports(String transports) {
    this.transports = transports;
  }

  public boolean isBackupEligible() {
    return "Y".equalsIgnoreCase(backupEligible);
  }

  public void setBackupEligible(boolean eligible) {
    this.backupEligible = eligible ? "Y" : "N";
  }

  public boolean isBackupState() {
    return "Y".equalsIgnoreCase(backupState);
  }

  public void setBackupState(boolean backedUp) {
    this.backupState = backedUp ? "Y" : "N";
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt != null ? createdAt : new Date();
  }

  public Date getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Date lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
