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

/**
 * JPA entity for {@code PSX_AUTH_LOGIN_TX}. Never expose {@code challengeB64} or {@code payloadJson}
 * in {@code toString}.
 */
@Entity
@Table(name = "PSX_AUTH_LOGIN_TX")
public class PSAuthLoginTx implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String TYPE_MFA_CHALLENGE = "MFA_CHALLENGE";
  public static final String TYPE_MFA_ENROLL = "ENROLL_ONLY";
  public static final String TYPE_WEBAUTHN_REG = "WEBAUTHN_REG";
  public static final String TYPE_WEBAUTHN_AUTH = "WEBAUTHN_AUTH";

  public static final String STATE_PENDING = "PENDING";
  public static final String STATE_VERIFIED = "VERIFIED";
  public static final String STATE_EXPIRED = "EXPIRED";
  public static final String STATE_CANCELLED = "CANCELLED";

  @Id
  @Column(name = "TX_ID", nullable = false, length = 64)
  private String txId;

  @Column(name = "TX_TYPE", nullable = false, length = 32)
  private String txType;

  @Column(name = "USER_NAME", length = 255)
  private String userName;

  @Column(name = "CHALLENGE_B64", length = 256)
  private String challengeB64;

  @Lob
  @Column(name = "PAYLOAD_JSON")
  private String payloadJson;

  @Column(name = "STATE", nullable = false, length = 32)
  private String state = STATE_PENDING;

  @Column(name = "ATTEMPT_COUNT", nullable = false)
  private int attemptCount;

  @Column(name = "CREATED_AT", nullable = false)
  private Date createdAt = new Date();

  @Column(name = "EXPIRES_AT", nullable = false)
  private Date expiresAt;

  public String getTxId() {
    return txId;
  }

  public void setTxId(String txId) {
    this.txId = txId;
  }

  public String getTxType() {
    return txType;
  }

  public void setTxType(String txType) {
    this.txType = txType;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getChallengeB64() {
    return challengeB64;
  }

  public void setChallengeB64(String challengeB64) {
    this.challengeB64 = challengeB64;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(int attemptCount) {
    this.attemptCount = attemptCount;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt != null ? createdAt : new Date();
  }

  public Date getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Date expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public String toString() {
    return "PSAuthLoginTx{txId='"
        + txId
        + "', txType='"
        + txType
        + "', userName='"
        + userName
        + "', state='"
        + state
        + "', challengeB64=[REDACTED]}";
  }
}
