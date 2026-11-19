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
package com.percussion.services.auth.mail;

/**
 * Synchronous transactional mail service dedicated to authentication, MFA, and recovery workflows.
 *
 * <p>Deliberately decoupled from workflow notifications and never honors workflow {@code
 * ENABLE_TEST_RUN_MODE} logging.
 */
public interface IPSAuthMailService {

  /** Resolves the effective auth mail configuration from server and optional workflow settings. */
  PSAuthMailConfig resolveEffectiveConfig();

  /** Sends a synchronous plain-text transactional authentication email. */
  void sendTextEmail(String toAddress, String subject, String plainTextBody)
      throws PSAuthMailException;

  /** Sends a non-secret diagnostic message to verify synchronous SMTP delivery. */
  void sendTestEmail(String toAddress) throws PSAuthMailException;
}
