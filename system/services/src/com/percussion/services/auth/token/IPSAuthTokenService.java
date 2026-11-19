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

import java.time.Duration;
import java.util.Optional;

/** Issues and atomically consumes CSPRNG-backed single-use authentication and recovery tokens. */
public interface IPSAuthTokenService {

  /**
   * Invalidates any prior unconsumed tokens for {@code (userName, tokenType)}, persists a SHA-256
   * hash of a fresh 256-bit token, and returns the raw token for one-time delivery.
   */
  PSIssuedAuthToken issueToken(
      String tokenType,
      String userName,
      String targetEmail,
      String createdBy,
      String requestIp,
      Duration ttl);

  /**
   * Atomically consumes {@code rawToken} if valid, unconsumed, unexpired, and matching {@code
   * expectedTokenType}. Never returns a match twice for concurrent or repeated submissions.
   */
  Optional<PSConsumedAuthToken> consumeToken(String rawToken, String expectedTokenType);

  /** Resolves configured token TTL from {@code server.properties} for the given token type. */
  Duration resolveTtl(String tokenType);
}
