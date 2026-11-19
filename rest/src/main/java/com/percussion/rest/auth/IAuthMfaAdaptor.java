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
package com.percussion.rest.auth;

/** Bridge interface connecting {@link AuthMfaResource} to {@code sitemanage} MFA services. */
public interface IAuthMfaAdaptor {

  ProfileMfaStatus getMyMfaStatus();

  MfaStepUpResponse verifyStepUp(MfaStepUpRequest request);

  TotpEnrollStartResponse startTotpEnrollment(TotpEnrollStartRequest request);

  TotpEnrollConfirmResponse confirmTotpEnrollment(TotpEnrollConfirmRequest request);

  MfaActionResult disableTotp();

  RecoveryCodesRegenerateResponse regenerateRecoveryCodes();

  PasskeyListResponse listPasskeys();

  PasskeyOptionsResponse startPasskeyRegistration(PasskeyOptionsRequest request);

  PasskeyRegisterFinishResponse finishPasskeyRegistration(PasskeyRegisterFinishRequest request);

  MfaActionResult renamePasskey(String passkeyId, PasskeyRenameRequest request);

  MfaActionResult deletePasskey(String passkeyId);

  PasskeyOptionsResponse startPasskeyStepUp();

  MfaStepUpResponse verifyPasskeyStepUp(PasskeyStepUpVerifyRequest request);
}
