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
package com.percussion.rest.test.apibridge;

import com.percussion.rest.auth.IAuthMfaAdaptor;
import com.percussion.rest.auth.MfaActionResult;
import com.percussion.rest.auth.MfaStepUpRequest;
import com.percussion.rest.auth.MfaStepUpResponse;
import com.percussion.rest.auth.PasskeyListResponse;
import com.percussion.rest.auth.PasskeyOptionsRequest;
import com.percussion.rest.auth.PasskeyOptionsResponse;
import com.percussion.rest.auth.PasskeyRegisterFinishRequest;
import com.percussion.rest.auth.PasskeyRegisterFinishResponse;
import com.percussion.rest.auth.PasskeyRenameRequest;
import com.percussion.rest.auth.PasskeyStepUpVerifyRequest;
import com.percussion.rest.auth.PasskeySummary;
import com.percussion.rest.auth.ProfileMfaStatus;
import com.percussion.rest.auth.RecoveryCodesRegenerateResponse;
import com.percussion.rest.auth.TotpEnrollConfirmRequest;
import com.percussion.rest.auth.TotpEnrollConfirmResponse;
import com.percussion.rest.auth.TotpEnrollStartRequest;
import com.percussion.rest.auth.TotpEnrollStartResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/** Spring test stub for {@link IAuthMfaAdaptor} (MainTest / shared rest context). */
@Component
public class TestAuthMfaAdaptor implements IAuthMfaAdaptor {

  @Override
  public ProfileMfaStatus getMyMfaStatus() {
    ProfileMfaStatus status = new ProfileMfaStatus();
    status.setUserName("Admin");
    status.setMfaMode("optional");
    status.setMfaRequired(false);
    status.setTotpEnabled(false);
    status.setPasskeyCount(0);
    status.setRecoveryCodesRemaining(0);
    status.setCanDisableTotp(true);
    status.setStepUpActive(false);
    return status;
  }

  @Override
  public MfaStepUpResponse verifyStepUp(MfaStepUpRequest request) {
    return new MfaStepUpResponse(true, System.currentTimeMillis() + 300_000L, "Verified");
  }

  @Override
  public TotpEnrollStartResponse startTotpEnrollment(TotpEnrollStartRequest request) {
    return new TotpEnrollStartResponse(
        "totp-stub-1",
        "otpauth://totp/Percussion:Admin?secret=JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
        "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
        "Percussion CMS",
        "Admin",
        6,
        30);
  }

  @Override
  public TotpEnrollConfirmResponse confirmTotpEnrollment(TotpEnrollConfirmRequest request) {
    return new TotpEnrollConfirmResponse(true, List.of("11111-AAAAA", "22222-BBBBB"), "Enabled");
  }

  @Override
  public MfaActionResult disableTotp() {
    return new MfaActionResult(true, "TOTP disabled");
  }

  @Override
  public RecoveryCodesRegenerateResponse regenerateRecoveryCodes() {
    return new RecoveryCodesRegenerateResponse(
        List.of("33333-CCCCC", "44444-DDDDD"), "Regenerated");
  }

  @Override
  public PasskeyListResponse listPasskeys() {
    return new PasskeyListResponse(
        List.of(
            new PasskeySummary(
                "pk-stub-1",
                "Touch ID",
                "00000000-0000-0000-0000-000000000000",
                List.of("internal"),
                true,
                true,
                Instant.parse("2026-11-19T00:00:00Z"),
                null)),
        true);
  }

  @Override
  public PasskeyOptionsResponse startPasskeyRegistration(PasskeyOptionsRequest request) {
    return new PasskeyOptionsResponse("reg-stub-1", "{\"challenge\":\"stub\"}");
  }

  @Override
  public PasskeyRegisterFinishResponse finishPasskeyRegistration(
      PasskeyRegisterFinishRequest request) {
    return new PasskeyRegisterFinishResponse(
        true, listPasskeys().getPasskeys().get(0), List.of(), "Registered");
  }

  @Override
  public MfaActionResult renamePasskey(String passkeyId, PasskeyRenameRequest request) {
    return new MfaActionResult(true, "Passkey renamed");
  }

  @Override
  public MfaActionResult deletePasskey(String passkeyId) {
    return new MfaActionResult(true, "Passkey removed");
  }

  @Override
  public PasskeyOptionsResponse startPasskeyStepUp() {
    return new PasskeyOptionsResponse("su-stub-1", "{\"challenge\":\"stub\"}");
  }

  @Override
  public MfaStepUpResponse verifyPasskeyStepUp(PasskeyStepUpVerifyRequest request) {
    return new MfaStepUpResponse(true, System.currentTimeMillis() + 300_000L, "Verified");
  }
}
