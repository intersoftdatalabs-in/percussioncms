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
package com.percussion.services.auth.mfa;

import java.time.Instant;

/** Thrown when an account is temporarily locked after repeated failed MFA attempts. */
public class PSMfaLockoutException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final Instant lockedUntil;

  public PSMfaLockoutException(String message, Instant lockedUntil) {
    super(message);
    this.lockedUntil = lockedUntil;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }
}
