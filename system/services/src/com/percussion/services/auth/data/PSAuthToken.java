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

/** JPA entity for {@code PSX_AUTH_TOKEN}. Never expose {@code tokenHash} in {@code toString}. */
@Entity
@Table(name = "PSX_AUTH_TOKEN")
public class PSAuthToken implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String TYPE_EMAIL_VERIFY = "EMAIL_VERIFY";
  public static final String TYPE_PASSWORD_RESET = "PASSWORD_RESET";
  public static final String TYPE_ADMIN_BREAK_GLASS = "ADMIN_BREAK_GLASS";

  @Id
  @Column(name = "TOKEN_ID", nullable = false, length = 64)
  private String tokenId;

  @Column(name = "TOKEN_HASH", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "TOKEN_TYPE", nullable = false, length = 32)
  private String tokenType;

  @Column(name = "USER_NAME", nullable = false, length = 255)
  private String userName;

  @Column(name = "TARGET_EMAIL", length = 320)
  private String targetEmail;

  @Column(name = "CREATED_BY", length = 255)
  private String createdBy;

  @Column(name = "REQUEST_IP", length = 64)
  private String requestIp;

  @Column(name = "CREATED_AT", nullable = false)
  private Date createdAt = new Date();

  @Column(name = "EXPIRES_AT", nullable = false)
  private Date expiresAt;

  @Column(name = "CONSUMED_AT")
  private Date consumedAt;

  public String getTokenId() {
    return tokenId;
  }

  public void setTokenId(String tokenId) {
    this.tokenId = tokenId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getTargetEmail() {
    return targetEmail;
  }

  public void setTargetEmail(String targetEmail) {
    this.targetEmail = targetEmail;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getRequestIp() {
    return requestIp;
  }

  public void setRequestIp(String requestIp) {
    this.requestIp = requestIp;
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

  public Date getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(Date consumedAt) {
    this.consumedAt = consumedAt;
  }

  @Override
  public String toString() {
    return "PSAuthToken{tokenId='"
        + tokenId
        + "', tokenType='"
        + tokenType
        + "', userName='"
        + userName
        + "', tokenHash=[REDACTED]}";
  }
}
