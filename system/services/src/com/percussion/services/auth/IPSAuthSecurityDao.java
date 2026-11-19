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
import com.percussion.services.auth.data.PSMfaTotp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistence contract for authentication, MFA factors, recovery codes, and tokens. */
public interface IPSAuthSecurityDao {

  Optional<PSAccountSecurity> findAccountSecurity(String userName);

  List<PSAccountSecurity> findAllAccountSecurity();

  List<PSAccountSecurity> findByNormalizedEmail(String emailNormalized);

  PSAccountSecurity saveAccountSecurity(PSAccountSecurity entity);

  List<PSMfaTotp> findActiveTotpByUser(String userName);

  List<PSMfaPasskey> findActivePasskeysByUser(String userName);

  long countUnusedRecoveryCodes(String userName);

  Map<String, Integer> countActiveTotpByUsers();

  Map<String, Integer> countActivePasskeysByUsers();

  Map<String, Integer> countUnusedRecoveryCodesByUsers();

  PSAuthToken saveToken(PSAuthToken token);

  Optional<PSAuthToken> findTokenByHash(String tokenHash);

  int purgeExpiredTokens(Instant cutoff);

  PSAuthLoginTx saveLoginTx(PSAuthLoginTx tx);

  Optional<PSAuthLoginTx> findLoginTx(String txId);

  int purgeExpiredLoginTx(Instant cutoff);

  Map<String, Boolean> checkSchemaTablesPresent();
}
