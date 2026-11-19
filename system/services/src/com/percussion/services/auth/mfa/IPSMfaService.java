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

import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import java.util.List;

/** Coordinates MFA challenge/enrollment transactions, TOTP, recovery codes, lockouts, and step-up. */
public interface IPSMfaService {

  String SESSION_ATTR_STEP_UP_UNTIL = "RX_MFA_STEP_UP_UNTIL";

  PSMfaTxStatus createLoginTransaction(
      String txType,
      String userName,
      String redirectUrl,
      String locale,
      Collection<String> roles);

  PSMfaTxStatus getTransactionStatus(String txId);

  PSMfaChallengeOutcome verifyChallengeTotp(String txId, String code, String requestIp);

  PSMfaChallengeOutcome verifyChallengeRecoveryCode(String txId, String code, String requestIp);

  PSTotpEnrollmentStart startEnrollOnlyTotp(String txId, String label);

  PSTotpConfirmOutcome completeEnrollOnlyTotp(
      String txId, String totpId, String code, String requestIp);

  boolean cancelTransaction(String txId);

  PSProfileMfaStatus getProfileMfaStatus(
      String userName, Collection<String> roles, HttpSession session);

  long verifyStepUp(String userName, String code, String requestIp, HttpSession session);

  long verifyStepUp(
      String userName, String code, boolean useRecoveryCode, String requestIp, HttpSession session);

  void requireStepUpIfEnrolled(String userName, HttpSession session);

  PSTotpEnrollmentStart startProfileTotpEnrollment(
      String userName, String label, HttpSession session);

  PSTotpConfirmOutcome confirmProfileTotpEnrollment(
      String userName, String totpId, String code, HttpSession session);

  void disableProfileTotp(String userName, Collection<String> roles, HttpSession session);

  List<String> regenerateRecoveryCodes(String userName, HttpSession session);

  PSPasskeyOptions startChallengePasskey(String txId, String requestOrigin);

  PSMfaChallengeOutcome verifyChallengePasskey(
      String txId,
      String challengeTxId,
      String credentialJson,
      String requestOrigin,
      String requestIp);

  PSPasskeyOptions startEnrollOnlyPasskey(String txId, String label, String requestOrigin);

  PSTotpConfirmOutcome completeEnrollOnlyPasskey(
      String txId,
      String challengeTxId,
      String label,
      String credentialJson,
      String requestOrigin,
      String requestIp);

  PSPasskeyOptions startProfilePasskeyStepUp(
      String userName, String requestOrigin, HttpSession session);

  long verifyProfilePasskeyStepUp(
      String userName,
      String challengeTxId,
      String credentialJson,
      String requestOrigin,
      String requestIp,
      HttpSession session);

  List<PSPasskeySummary> listProfilePasskeys(String userName);

  boolean canDeleteLastPasskey(String userName, Collection<String> roles);

  PSPasskeyOptions startProfilePasskeyRegistration(
      String userName,
      String displayName,
      String label,
      String requestOrigin,
      HttpSession session);

  PSPasskeyRegisterOutcome finishProfilePasskeyRegistration(
      String userName,
      String challengeTxId,
      String label,
      String credentialJson,
      String requestOrigin,
      HttpSession session);

  void renameProfilePasskey(String userName, String passkeyId, String newLabel);

  void deleteProfilePasskey(
      String userName, String passkeyId, Collection<String> roles, HttpSession session);
}
