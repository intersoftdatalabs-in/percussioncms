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
package com.percussion.services.auth.account;

/** Manages verified account email lifecycles, staged email changes, and username recovery. */
public interface IPSAccountSecurityService {

  /** Whether configured policy requires email addresses for interactive human accounts. */
  boolean isEmailRequired();

  /** Whether {@code userName} represents an internal non-interactive service account. */
  boolean isSystemServiceUser(String userName);

  /** Loads email verification state for {@code userName}, syncing directory email changes. */
  PSEmailVerificationStatus getEmailStatus(
      String userName, String currentSubjectEmail, boolean isDirectoryUser);

  /** Stages an internal email change and dispatches verification (and old-address notice). */
  PSEmailChangeResult stageEmailChange(
      String userName,
      String currentSubjectEmail,
      String newEmail,
      String requestIp,
      boolean isInternalUser);

  /** Resends a verification link for an unverified active or pending email address. */
  PSEmailChangeResult resendVerification(
      String userName, String currentSubjectEmail, String requestIp);

  /** Atomically consumes {@code rawToken} and promotes the target email to active + verified. */
  PSEmailVerifyResult confirmEmailVerification(String rawToken);

  /** Handles enumeration-safe, rate-limited Forgot Username requests for verified emails only. */
  PSForgotUsernameResult requestForgotUsername(String rawEmail, String requestIp);

  /** Synchronizes {@code PSX_ACCOUNT_SECURITY} when administrators create, update, or import users. */
  void syncAccountEmailOnAdminSave(String userName, String newEmail, boolean isDirectoryUser);
}
