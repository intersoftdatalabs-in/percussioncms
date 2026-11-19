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

import com.percussion.services.auth.data.PSMfaPasskey;
import java.util.List;

/**
 * WebAuthn / FIDO2 passkey registration, verification, and credential inventory service.
 */
public interface IPSPasskeyService {

  PSPasskeyOptions startRegistration(
      String userName, String displayName, String label, String requestOrigin);

  PSPasskeyRegisterOutcome finishRegistration(
      String userName,
      String challengeTxId,
      String label,
      String registrationResponseJson,
      String requestOrigin);

  PSPasskeyOptions startAuthentication(String userName, String requestOrigin);

  PSMfaPasskey verifyAuthentication(
      String expectedUserName,
      String challengeTxId,
      String authenticationResponseJson,
      String requestOrigin);

  List<PSPasskeySummary> listPasskeys(String userName);

  void renamePasskey(String userName, String passkeyId, String newLabel);

  void deletePasskey(String userName, String passkeyId);
}
