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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rest.auth.ProfileEmailActionResult;
import com.percussion.rest.auth.ProfileEmailChangeRequest;
import com.percussion.rest.auth.ProfileEmailStatus;
import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailChangeResult;
import com.percussion.services.auth.account.PSEmailVerificationStatus;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import com.percussion.user.data.PSCurrentUser;
import com.percussion.user.data.PSUserProviderType;
import com.percussion.user.service.IPSUserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

@Tag("UnitTest")
class AuthProfileAdaptorTest {

  private static PSCurrentUser makeCurrent(
      String name, PSUserProviderType provider, String email) {
    PSCurrentUser user = new PSCurrentUser();
    user.setName(name);
    user.setProviderType(provider);
    user.setEmail(email);
    user.setRoles(List.of("Editor"));
    return user;
  }

  @Test
  void returnsInternalAndDirectoryEmailStatusWithoutIdorParameters() throws Exception {
    IPSUserService userService = mock(IPSUserService.class);
    IPSAccountSecurityService accountService = mock(IPSAccountSecurityService.class);
    AuthProfileAdaptor adaptor =
        new AuthProfileAdaptor(userService, accountService, () -> "192.0.2.5");

    when(userService.getCurrentUser())
        .thenReturn(makeCurrent("editor1", PSUserProviderType.INTERNAL, "old@example.com"));
    when(accountService.getEmailStatus("editor1", "old@example.com", false))
        .thenReturn(
            new PSEmailVerificationStatus(
                "editor1",
                "old@example.com",
                true,
                Instant.parse("2026-11-18T10:00:00Z"),
                "new@example.com",
                "INTERACTIVE_MFA_REQUIRED",
                true));

    ProfileEmailStatus internalStatus = adaptor.getMyEmailStatus();
    assertTrue(internalStatus.isEmailEditable());
    assertTrue(internalStatus.isEmailVerified());
    assertEquals("new@example.com", internalStatus.getPendingEmail());
    assertEquals("INTERNAL", internalStatus.getProviderType());

    when(userService.getCurrentUser())
        .thenReturn(makeCurrent("ldap.user", PSUserProviderType.DIRECTORY, "ldap@corp.example"));
    when(accountService.getEmailStatus("ldap.user", "ldap@corp.example", true))
        .thenReturn(
            new PSEmailVerificationStatus(
                "ldap.user",
                "ldap@corp.example",
                false,
                null,
                "",
                "INTERACTIVE_MFA_REQUIRED",
                true));

    ProfileEmailStatus dirStatus = adaptor.getMyEmailStatus();
    assertFalse(dirStatus.isEmailEditable());
    assertFalse(dirStatus.isEmailVerified());
    assertEquals("DIRECTORY", dirStatus.getProviderType());
  }

  @Test
  void delegatesStageEmailChangeResendAndPropagatesRateLimit() throws Exception {
    IPSUserService userService = mock(IPSUserService.class);
    IPSAccountSecurityService accountService = mock(IPSAccountSecurityService.class);
    AuthProfileAdaptor adaptor =
        new AuthProfileAdaptor(userService, accountService, () -> "203.0.113.10");

    when(userService.getCurrentUser())
        .thenReturn(makeCurrent("editor1", PSUserProviderType.INTERNAL, "old@example.com"));
    when(accountService.stageEmailChange(
            "editor1", "old@example.com", "new@example.com", "203.0.113.10", true))
        .thenReturn(
            new PSEmailChangeResult(
                true, false, "old@example.com", "new@example.com", true, "Sent"));

    ProfileEmailActionResult changed =
        adaptor.requestMyEmailChange(new ProfileEmailChangeRequest("new@example.com"));
    assertTrue(changed.isVerificationSent());
    assertEquals("old@example.com", changed.getEmail());
    assertEquals("new@example.com", changed.getPendingEmail());

    when(accountService.resendVerification("editor1", "old@example.com", "203.0.113.10"))
        .thenThrow(new PSAuthRateLimitException());
    assertThrows(PSAuthRateLimitException.class, adaptor::resendMyEmailVerification);
    verify(accountService).resendVerification("editor1", "old@example.com", "203.0.113.10");
  }

  @Test
  void requiresStepUpMfaBeforeChangingAnAlreadyVerifiedEmail() throws Exception {
    IPSUserService userService = mock(IPSUserService.class);
    IPSAccountSecurityService accountService = mock(IPSAccountSecurityService.class);
    IPSMfaService mfaService = mock(IPSMfaService.class);
    MockHttpSession session = new MockHttpSession();
    AuthProfileAdaptor adaptor =
        new AuthProfileAdaptor(userService, accountService, () -> session, () -> "203.0.113.10");
    adaptor.setMfaService(mfaService);

    when(userService.getCurrentUser())
        .thenReturn(makeCurrent("editor1", PSUserProviderType.INTERNAL, "old@example.com"));
    when(accountService.getEmailStatus("editor1", "old@example.com", false))
        .thenReturn(
            new PSEmailVerificationStatus(
                "editor1",
                "old@example.com",
                true,
                Instant.parse("2026-11-18T10:00:00Z"),
                "",
                "INTERACTIVE_MFA_REQUIRED",
                true));
    org.mockito.Mockito.doThrow(new PSStepUpRequiredException("Step-up required"))
        .when(mfaService)
        .requireStepUpIfEnrolled("editor1", session);

    assertThrows(
        PSStepUpRequiredException.class,
        () -> adaptor.requestMyEmailChange(new ProfileEmailChangeRequest("new@example.com")));
  }
}
