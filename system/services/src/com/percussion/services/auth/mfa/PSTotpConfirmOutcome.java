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

import java.util.List;

/** Outcome of confirming a new TOTP authenticator enrollment. */
public record PSTotpConfirmOutcome(
    boolean enrolled,
    List<String> recoveryCodes,
    String userName,
    String redirectUrl,
    String locale,
    List<String> roles,
    String message) {

  @Override
  public String toString() {
    return "PSTotpConfirmOutcome[enrolled="
        + enrolled
        + ", recoveryCodeCount="
        + (recoveryCodes != null ? recoveryCodes.size() : 0)
        + ", userName="
        + userName
        + "]";
  }
}
