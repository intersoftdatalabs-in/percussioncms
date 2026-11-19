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
package com.percussion.services.auth.recovery;

import jakarta.servlet.http.HttpSession;

/**
 * Coordinates password reset recovery, password policy resolution, and audited Admin break-glass
 * MFA recovery.
 */
public interface IPSAccountRecoveryService {

  /** Resolves configured minimum password length (default 12, clamped 8..150). */
  int resolvePasswordMinLength();

  /**
   * Handles enumeration-safe, rate-limited Forgot Password requests. Sends reset links only to
   * verified internal account emails, and sends directory/SSO guidance for external accounts.
   */
  PSForgotPasswordResult requestPasswordReset(String identifier, String requestIp);

  /**
   * Validates the new password policy, atomically consumes {@code rawToken}, updates the internal
   * password, increments {@code SESSION_VERSION}, releases active sessions, and preserves MFA
   * factors.
   */
  PSPasswordResetResult completePasswordReset(
      String rawToken, String newPassword, String requestIp);

  /**
   * Issues an audited Admin break-glass MFA recovery link (or immediate factor reset) after
   * enforcing recent Admin step-up verification and a required verification note.
   */
  PSAdminRecoveryIssueResult issueAdminBreakGlassRecovery(
      String adminActor,
      String targetUserName,
      String verificationNote,
      boolean resetImmediately,
      String requestIp,
      HttpSession adminSession);

  /** Atomically redeems an Admin break-glass token, resetting MFA factors and active sessions. */
  PSAdminRecoveryRedeemResult redeemAdminBreakGlass(String rawToken, String requestIp);
}
