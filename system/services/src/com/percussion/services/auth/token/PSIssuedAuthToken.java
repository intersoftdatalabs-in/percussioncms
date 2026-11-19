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
package com.percussion.services.auth.token;

import java.time.Instant;
import java.util.Objects;

/**
 * Newly minted single-use authentication or verification token.
 *
 * <p>{@link #rawToken()} must be delivered only to the destination channel (for example in an email
 * verification URL) and never logged or stored in plaintext.
 */
public record PSIssuedAuthToken(
    String tokenId,
    String rawToken,
    String tokenType,
    String userName,
    String targetEmail,
    Instant expiresAt) {

  public PSIssuedAuthToken {
    Objects.requireNonNull(tokenId, "tokenId");
    Objects.requireNonNull(rawToken, "rawToken");
    Objects.requireNonNull(tokenType, "tokenType");
    Objects.requireNonNull(userName, "userName");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }

  @Override
  public String toString() {
    return "PSIssuedAuthToken{tokenId='"
        + tokenId
        + "', tokenType='"
        + tokenType
        + "', userName='"
        + userName
        + "', expiresAt="
        + expiresAt
        + ", rawToken=[REDACTED]}";
  }
}
