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
package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.auth.MfaStepUpRequest;
import com.percussion.rest.auth.MfaStepUpResponse;
import com.percussion.rest.auth.ProfileMfaStatus;
import com.percussion.rest.auth.RecoveryCodesRegenerateResponse;
import com.percussion.rest.auth.TotpEnrollConfirmRequest;
import com.percussion.rest.auth.TotpEnrollConfirmResponse;
import com.percussion.rest.auth.TotpEnrollStartRequest;
import com.percussion.rest.auth.TotpEnrollStartResponse;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSProfileMfaStatus;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

@Tag("UnitTest")
class AuthMfaAdaptorTest {

  private static PSCurrentUser makeCurrent(String name) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setProviderType(PSUserProviderType.INTERNAL);
    user.setRoles(List.of("Editor"));
    return user;
  }

  @Test
  void enforcesSessionUserAndDelegatesMfaLifecycleOperations() throws Exception {
    IPSUserService userService = mock(IPSUserService.class);
    IPSMfaService mfaService = mock(IPSMfaService.class);
    MockHttpSession session = new MockHttpSession();
    AuthMfaAdaptor adaptor =
        new AuthMfaAdaptor(userService, mfaService, () -> session, () -> "203.0.113.9");

    when(userService.getCurrentUser()).thenReturn(makeCurrent("editor1"));
    when(mfaService.getProfileMfaStatus("editor1", List.of("Editor"), session))
        .thenReturn(
            new PSProfileMfaStatus(
                "editor1",
                "optional",
                false,
                true,
                0,
                10,
                true,
                true,
                Instant.parse("2026-11-18T12:05:00Z")));

    ProfileMfaStatus status = adaptor.getMyMfaStatus();
    assertEquals("editor1", status.getUserName());
    assertTrue(status.isTotpEnabled());
    assertTrue(status.isStepUpActive());

    when(mfaService.verifyStepUp("editor1", "123456", false, "203.0.113.9", session))
        .thenReturn(1795000000000L);
    MfaStepUpResponse stepUp = adaptor.verifyStepUp(new MfaStepUpRequest("123456", false));
    assertTrue(stepUp.isVerified());

    when(mfaService.startProfileTotpEnrollment("editor1", "Work Phone", session))
        .thenReturn(
            new PSTotpEnrollmentStart(
                "t1", "otpauth://totp/Percussion:editor1?secret=ABC", "ABC", "Percussion", "editor1", 6, 30));
    TotpEnrollStartResponse start = adaptor.startTotpEnrollment(new TotpEnrollStartRequest("Work Phone"));
    assertEquals("t1", start.getTotpId());

    when(mfaService.confirmProfileTotpEnrollment("editor1", "t1", "123456", session))
        .thenReturn(
            new PSTotpConfirmOutcome(true, List.of("11111-AAAAA"), "editor1", "", "", List.of(), "ok"));
    TotpEnrollConfirmResponse confirm =
        adaptor.confirmTotpEnrollment(new TotpEnrollConfirmRequest("t1", "123456"));
    assertTrue(confirm.isEnrolled());

    doThrow(new PSStepUpRequiredException("Step-up required"))
        .when(mfaService)
        .disableProfileTotp("editor1", List.of("Editor"), session);
    assertThrows(PSStepUpRequiredException.class, adaptor::disableTotp);

    when(mfaService.regenerateRecoveryCodes("editor1", session))
        .thenReturn(List.of("22222-BBBBB"));
    RecoveryCodesRegenerateResponse regen = adaptor.regenerateRecoveryCodes();
    assertEquals(1, regen.getRecoveryCodes().size());
  }
}
