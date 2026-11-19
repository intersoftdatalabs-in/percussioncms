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

/** JPA entity for {@code PSX_MFA_RECOVERY_CODE}. Never expose hash/salt in {@code toString}. */
@Entity
@Table(name = "PSX_MFA_RECOVERY_CODE")
public class PSMfaRecoveryCode implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @Column(name = "CODE_ID", nullable = false, length = 64)
  private String codeId;

  @Column(name = "USER_NAME", nullable = false, length = 255)
  private String userName;

  @Column(name = "CODE_HASH", nullable = false, length = 256)
  private String codeHash;

  @Column(name = "CODE_SALT", nullable = false, length = 64)
  private String codeSalt;

  @Column(name = "BATCH_ID", nullable = false, length = 64)
  private String batchId;

  @Column(name = "CREATED_AT", nullable = false)
  private Date createdAt = new Date();

  @Column(name = "USED_AT")
  private Date usedAt;

  public String getCodeId() {
    return codeId;
  }

  public void setCodeId(String codeId) {
    this.codeId = codeId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public void setCodeHash(String codeHash) {
    this.codeHash = codeHash;
  }

  public String getCodeSalt() {
    return codeSalt;
  }

  public void setCodeSalt(String codeSalt) {
    this.codeSalt = codeSalt;
  }

  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt != null ? createdAt : new Date();
  }

  public Date getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Date usedAt) {
    this.usedAt = usedAt;
  }

  @Override
  public String toString() {
    return "PSMfaRecoveryCode{codeId='"
        + codeId
        + "', userName='"
        + userName
        + "', batchId='"
        + batchId
        + "', codeHash=[REDACTED]}";
  }
}
