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
package com.percussion.services.auth;

import com.percussion.services.auth.data.PSAccountSecurity;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.data.PSAuthToken;
import com.percussion.services.auth.data.PSMfaPasskey;
import com.percussion.services.auth.data.PSMfaRecoveryCode;
import com.percussion.services.auth.data.PSMfaTotp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence contract for authentication, MFA factors, recovery codes, and tokens. */
public interface IPSAuthSecurityDao {

  Optional<PSAccountSecurity> findAccountSecurity(String userName);

  List<PSAccountSecurity> findAllAccountSecurity();

  List<PSAccountSecurity> findByNormalizedEmail(String emailNormalized);

  List<PSAccountSecurity> findVerifiedByNormalizedEmail(String emailNormalized);

  PSAccountSecurity saveAccountSecurity(PSAccountSecurity entity);

  List<PSMfaTotp> findActiveTotpByUser(String userName);

  List<PSMfaPasskey> findActivePasskeysByUser(String userName);

  long countUnusedRecoveryCodes(String userName);

  Map<String, Integer> countActiveTotpByUsers();

  Map<String, Integer> countActivePasskeysByUsers();

  Map<String, Integer> countUnusedRecoveryCodesByUsers();

  PSAuthToken saveToken(PSAuthToken token);

  Optional<PSAuthToken> findTokenByHash(String tokenHash);

  int consumeAuthTokenAtomically(String tokenHash, String tokenType, Instant consumedAt);

  int invalidateUnconsumedTokens(String userName, String tokenType, Instant consumedAt);

  int purgeExpiredTokens(Instant cutoff);

  PSAuthLoginTx saveLoginTx(PSAuthLoginTx tx);

  Optional<PSAuthLoginTx> findLoginTx(String txId);

  int consumeLoginTxAtomically(String txId, String expectedType, Date now);

  int incrementLoginTxAttemptsAtomically(String txId, int maxAttempts, Date now);

  int cancelLoginTx(String txId);

  int purgeExpiredLoginTx(Instant cutoff);

  PSMfaTotp saveTotp(PSMfaTotp totp);

  Optional<PSMfaTotp> findTotpById(String totpId);

  List<PSMfaTotp> findTotpByUserAndStatus(String userName, String status);

  int advanceTotpLastUsedStepAtomically(String totpId, long newStep, Date usedAt);

  int revokeTotpByUser(String userName, String currentStatus);

  void saveRecoveryCodes(List<PSMfaRecoveryCode> codes);

  List<PSMfaRecoveryCode> findUnusedRecoveryCodesByUser(String userName);

  int consumeRecoveryCodeAtomically(String codeId, Date usedAt);

  int invalidateUnusedRecoveryCodesByUser(String userName, Date usedAt);

  long findSessionVersion(String userName);

  long incrementSessionVersion(String userName);

  PSMfaPasskey savePasskey(PSMfaPasskey passkey);

  Optional<PSMfaPasskey> findPasskeyById(String passkeyId);

  Optional<PSMfaPasskey> findActivePasskeyByCredentialId(String credentialId);

  int renamePasskey(String passkeyId, String userName, String newLabel);

  int revokePasskey(String passkeyId, String userName);

  int updatePasskeySignCountAtomically(
      String passkeyId,
      long expectedSignCount,
      long newSignCount,
      boolean backupEligible,
      boolean backupState,
      Date lastUsedAt);

  Map<String, Boolean> checkSchemaTablesPresent();
}
