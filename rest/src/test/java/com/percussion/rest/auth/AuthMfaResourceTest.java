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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.rest.JacksonContextResolver;
import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSStepUpRequiredException;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Tag("UnitTest")
class AuthMfaResourceTest {

  @Test
  void delegatesMfaOperationsAndMapsErrorStatuses() {
    IAuthMfaAdaptor adaptor = mock(IAuthMfaAdaptor.class);
    ProfileMfaStatus status = new ProfileMfaStatus();
    status.setUserName("editor1");
    status.setMfaMode("optional");
    status.setTotpEnabled(true);
    when(adaptor.getMyMfaStatus()).thenReturn(status);

    AuthMfaResource resource = new AuthMfaResource(adaptor);
    assertSame(status, resource.getMyMfaStatus());

    TotpEnrollStartRequest startReq = new TotpEnrollStartRequest("Phone");
    TotpEnrollStartResponse startRes =
        new TotpEnrollStartResponse(
            "t1", "otpauth://totp/Percussion:editor1?secret=ABC", "ABC", "Percussion", "editor1", 6, 30);
    assertFalse(startRes.toString().contains("ABC"));
    when(adaptor.startTotpEnrollment(startReq)).thenReturn(startRes);
    assertSame(startRes, resource.startTotpEnrollment(startReq));

    TotpEnrollConfirmRequest confirmReq = new TotpEnrollConfirmRequest("t1", "123456");
    TotpEnrollConfirmResponse confirmRes =
        new TotpEnrollConfirmResponse(true, List.of("11111-AAAAA"), "Enabled");
    when(adaptor.confirmTotpEnrollment(confirmReq)).thenReturn(confirmRes);
    assertSame(confirmRes, resource.confirmTotpEnrollment(confirmReq));

    doThrow(new PSStepUpRequiredException("Step up required")).when(adaptor).disableTotp();
    WebApplicationException ex428 =
        assertThrows(WebApplicationException.class, resource::disableTotp);
    assertEquals(428, ex428.getResponse().getStatus());

    doThrow(new PSMfaLockoutException("Locked", Instant.now().plusSeconds(900)))
        .when(adaptor)
        .verifyStepUp(org.mockito.ArgumentMatchers.any());
    assertEquals(
        423,
        assertThrows(
                WebApplicationException.class,
                () -> resource.verifyStepUp(new MfaStepUpRequest("000000", false)))
            .getResponse()
            .getStatus());

    doThrow(new PSAuthRateLimitException()).when(adaptor).regenerateRecoveryCodes();
    assertEquals(
        429,
        assertThrows(WebApplicationException.class, resource::regenerateRecoveryCodes)
            .getResponse()
            .getStatus());

    doThrow(new IllegalStateException("Last factor")).when(adaptor).disableTotp();
    assertEquals(
        409,
        assertThrows(WebApplicationException.class, resource::disableTotp)
            .getResponse()
            .getStatus());

    doThrow(new SecurityException("No session")).when(adaptor).getMyMfaStatus();
    assertEquals(
        401,
        assertThrows(WebApplicationException.class, resource::getMyMfaStatus)
            .getResponse()
            .getStatus());

    doReturn(new MfaActionResult(true, "Disabled")).when(adaptor).disableTotp();
    assertTrue(resource.disableTotp().isSuccess());

    assertEquals(
        503,
        assertThrows(WebApplicationException.class, () -> new AuthMfaResource().getMyMfaStatus())
            .getResponse()
            .getStatus());
  }

  @Test
  void jacksonContextResolverWrapsMfaDtosWithRootNames() {
    ObjectMapper mapper = new JacksonContextResolver().getContext(ProfileMfaStatus.class);
    ObjectMapper treeMapper = JsonMapper.builder().build();

    ProfileMfaStatus status = new ProfileMfaStatus();
    status.setUserName("editor1");
    status.setMfaMode("cohort");
    status.setMfaRequired(true);
    status.setTotpEnabled(true);
    status.setRecoveryCodesRemaining(10);

    JsonNode root = treeMapper.readTree(mapper.writeValueAsString(status));
    assertTrue(root.has("ProfileMfaStatus"));
    assertEquals("cohort", root.get("ProfileMfaStatus").get("mfaMode").asString());
    assertEquals(10, root.get("ProfileMfaStatus").get("recoveryCodesRemaining").asInt());

    MfaStepUpRequest decoded =
        mapper.readValue(
            "{\"MfaStepUpRequest\":{\"code\":\"123456\",\"recoveryCode\":false}}",
            MfaStepUpRequest.class);
    assertEquals("123456", decoded.getCode());
    assertFalse(decoded.isRecoveryCode());

    PasskeyRegisterFinishRequest pkDecoded =
        mapper.readValue(
            "{\"PasskeyRegisterFinishRequest\":{\"challengeTxId\":\"tx1\",\"label\":\"Laptop\",\"credentialJson\":\"{}\"}}",
            PasskeyRegisterFinishRequest.class);
    assertEquals("tx1", pkDecoded.getChallengeTxId());
    assertEquals("Laptop", pkDecoded.getLabel());
  }

  @Test
  void delegatesPasskeyEndpointsAndMapsLastFactorConflict() {
    IAuthMfaAdaptor adaptor = mock(IAuthMfaAdaptor.class);
    AuthMfaResource resource = new AuthMfaResource(adaptor);

    PasskeySummary pk =
        new PasskeySummary("pk-1", "YubiKey", "0000", List.of("usb"), false, false, Instant.now(), null);
    when(adaptor.listPasskeys()).thenReturn(new PasskeyListResponse(List.of(pk), false));
    assertFalse(resource.listPasskeys().isCanDeleteLastPasskey());

    PasskeyOptionsRequest optReq = new PasskeyOptionsRequest("YubiKey");
    when(adaptor.startPasskeyRegistration(optReq))
        .thenReturn(new PasskeyOptionsResponse("ctx1", "{\"challenge\":\"abc\"}"));
    assertEquals("ctx1", resource.startPasskeyRegistration(optReq).getChallengeTxId());

    PasskeyRegisterFinishRequest finReq = new PasskeyRegisterFinishRequest("ctx1", "YubiKey", "{}");
    when(adaptor.finishPasskeyRegistration(finReq))
        .thenReturn(new PasskeyRegisterFinishResponse(true, pk, List.of("11111-AAAAA"), "Added"));
    assertTrue(resource.finishPasskeyRegistration(finReq).isRegistered());

    PasskeyRenameRequest renReq = new PasskeyRenameRequest("Work Key");
    when(adaptor.renamePasskey("pk-1", renReq)).thenReturn(new MfaActionResult(true, "Renamed"));
    assertTrue(resource.renamePasskey("pk-1", renReq).isSuccess());

    doThrow(new IllegalStateException("Last MFA factor")).when(adaptor).deletePasskey("pk-1");
    assertEquals(
        409,
        assertThrows(WebApplicationException.class, () -> resource.deletePasskey("pk-1"))
            .getResponse()
            .getStatus());

    when(adaptor.startPasskeyStepUp()).thenReturn(new PasskeyOptionsResponse("su1", "{}"));
    assertEquals("su1", resource.startPasskeyStepUp().getChallengeTxId());

    PasskeyStepUpVerifyRequest suReq = new PasskeyStepUpVerifyRequest("su1", "{}");
    when(adaptor.verifyPasskeyStepUp(suReq))
        .thenReturn(new MfaStepUpResponse(true, 1795000000000L, "Verified"));
    assertTrue(resource.verifyPasskeyStepUp(suReq).isVerified());
  }
}
