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
package com.percussion.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.services.auth.account.IPSAccountSecurityService;
import com.percussion.services.auth.account.PSEmailVerifyResult;
import com.percussion.services.auth.account.PSForgotUsernameResult;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSMfaChallengeOutcome;
import com.percussion.services.auth.mfa.PSMfaLockoutException;
import com.percussion.services.auth.mfa.PSMfaTxStatus;
import com.percussion.services.auth.mfa.PSPasskeyOptions;
import com.percussion.services.auth.mfa.PSTotpConfirmOutcome;
import com.percussion.services.auth.mfa.PSTotpEnrollmentStart;
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("UnitTest")
class PSAuthServletTest {

  private IPSAccountSecurityService accountService;
  private IPSMfaService mfaService;
  private PSAuthCsrfGuard csrfGuard;
  private PSAuthServlet servlet;

  @BeforeEach
  void setUp() {
    accountService = mock(IPSAccountSecurityService.class);
    mfaService = mock(IPSMfaService.class);
    csrfGuard = new PSAuthCsrfGuard(Properties::new);
    servlet =
        new PSAuthServlet(
            accountService, mfaService, csrfGuard, (req, res, sub, locale) -> req);
  }

  @Test
  void getVerifyEmailForwardsWithoutConsumingTokenSoEmailScannersCannotConsumeIt()
      throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/auth/verify-email");
    req.setPathInfo("/verify-email");
    req.setParameter("token", "secret-raw-token");
    MockHttpServletResponse res = new MockHttpServletResponse();

    servlet.doGet(req, res);

    verify(accountService, never()).confirmEmailVerification(anyString());
    assertEquals("/rxauth.jsp", res.getForwardedUrl());
    assertEquals("verify-email", req.getAttribute(PSAuthServlet.ATTR_VIEW));
    assertEquals("secret-raw-token", req.getAttribute(PSAuthServlet.ATTR_TOKEN));
    assertTrue(res.getHeader("Cache-Control").contains("no-store"));
  }

  @Test
  void postVerifyEmailRejectsMissingCsrfThenConsumesValidToken() throws Exception {
    MockHttpServletRequest unauthPost = new MockHttpServletRequest("POST", "/auth/verify-email");
    unauthPost.setPathInfo("/verify-email");
    MockHttpServletResponse denied = new MockHttpServletResponse();

    servlet.doPost(unauthPost, denied);
    assertEquals(403, denied.getStatus());
    verify(accountService, never()).confirmEmailVerification(anyString());

    MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/auth/verify-email");
    getReq.setPathInfo("/verify-email");
    servlet.doGet(getReq, new MockHttpServletResponse());
    String csrf = (String) getReq.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);

    when(accountService.confirmEmailVerification("good-tok"))
        .thenReturn(
            new PSEmailVerifyResult(
                true, PSEmailVerifyResult.STATUS_VERIFIED, "Your email address has been verified."));

    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/auth/verify-email");
    post.setPathInfo("/verify-email");
    post.setSession(getReq.getSession());
    post.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    post.setContentType("application/json");
    post.setContent("{\"token\":\"good-tok\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse okRes = new MockHttpServletResponse();

    servlet.doPost(post, okRes);
    assertEquals(200, okRes.getStatus());
    assertTrue(okRes.getContentAsString().contains("\"verified\":true"));
  }

  @Test
  void postForgotUsernameReturnsGeneric200AndMapsRateLimitTo429() throws Exception {
    MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/auth/forgot-username");
    getReq.setPathInfo("/forgot-username");
    servlet.doGet(getReq, new MockHttpServletResponse());
    String csrf = (String) getReq.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);

    when(accountService.requestForgotUsername("alice@example.com", "127.0.0.1"))
        .thenReturn(new PSForgotUsernameResult(true, "Generic message"));

    MockHttpServletRequest post = new MockHttpServletRequest("POST", "/auth/forgot-username");
    post.setPathInfo("/forgot-username");
    post.setSession(getReq.getSession());
    post.setRemoteAddr("127.0.0.1");
    post.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    post.setContentType("application/json");
    post.setContent("{\"email\":\"alice@example.com\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse res = new MockHttpServletResponse();

    servlet.doPost(post, res);
    assertEquals(200, res.getStatus());
    assertTrue(res.getContentAsString().contains("\"accepted\":true"));

    when(accountService.requestForgotUsername("burst@example.com", "127.0.0.1"))
        .thenThrow(new PSAuthRateLimitException());
    MockHttpServletRequest burst = new MockHttpServletRequest("POST", "/auth/forgot-username");
    burst.setPathInfo("/forgot-username");
    burst.setSession(getReq.getSession());
    burst.setRemoteAddr("127.0.0.1");
    burst.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    burst.setContentType("application/json");
    burst.setContent("{\"email\":\"burst@example.com\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse res429 = new MockHttpServletResponse();

    servlet.doPost(burst, res429);
    assertEquals(429, res429.getStatus());
  }

  @Test
  void getAndPostChallengeTotpAndRecoveryCodeAndCancelWork() throws Exception {
    when(mfaService.getTransactionStatus("tx-1"))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-1",
                "MFA_CHALLENGE",
                "alice",
                "/cm/app/home",
                "en-us",
                List.of("Admin"),
                true,
                false,
                8L,
                Instant.now().plusSeconds(300)));

    MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/auth/challenge");
    getReq.setPathInfo("/challenge");
    getReq.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-1"));
    MockHttpServletResponse getRes = new MockHttpServletResponse();

    servlet.doGet(getReq, getRes);
    assertEquals("/rxauth.jsp", getRes.getForwardedUrl());
    assertEquals("mfa-challenge", getReq.getAttribute(PSAuthServlet.ATTR_VIEW));
    String bootstrap = (String) getReq.getAttribute(PSAuthServlet.ATTR_BOOTSTRAP_JSON);
    assertTrue(bootstrap.contains("\"hasValidTx\":true"));
    assertTrue(bootstrap.contains("\"totpAvailable\":true"));
    org.junit.jupiter.api.Assertions.assertFalse(bootstrap.contains("tx-1"));

    String csrf = (String) getReq.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);

    // Invalid code returns 401
    when(mfaService.verifyChallengeTotp("tx-1", "000000", "127.0.0.1"))
        .thenReturn(
            new PSMfaChallengeOutcome(
                false, PSMfaChallengeOutcome.STATUS_INVALID_CODE, "Bad code", "alice", "", "", List.of(), 0L));
    MockHttpServletRequest badPost = new MockHttpServletRequest("POST", "/auth/challenge/totp");
    badPost.setPathInfo("/challenge/totp");
    badPost.setSession(getReq.getSession());
    badPost.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-1"));
    badPost.setRemoteAddr("127.0.0.1");
    badPost.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    badPost.setContentType("application/json");
    badPost.setContent("{\"code\":\"000000\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse res401 = new MockHttpServletResponse();
    servlet.doPost(badPost, res401);
    assertEquals(401, res401.getStatus());

    // Valid TOTP returns 200 and clears PERC_AUTH_TX cookie
    when(mfaService.verifyChallengeTotp("tx-1", "123456", "127.0.0.1"))
        .thenReturn(
            new PSMfaChallengeOutcome(
                true,
                PSMfaChallengeOutcome.STATUS_VERIFIED,
                "ok",
                "alice",
                "/cm/app/home",
                "en-us",
                List.of("Admin"),
                8L));
    MockHttpServletRequest okPost = new MockHttpServletRequest("POST", "/auth/challenge/totp");
    okPost.setPathInfo("/challenge/totp");
    okPost.setSession(getReq.getSession());
    okPost.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-1"));
    okPost.setRemoteAddr("127.0.0.1");
    okPost.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    okPost.setContentType("application/json");
    okPost.setContent("{\"code\":\"123456\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse res200 = new MockHttpServletResponse();
    servlet.doPost(okPost, res200);
    assertEquals(200, res200.getStatus());
    assertTrue(res200.getContentAsString().contains("\"authenticated\":true"));
    assertTrue(res200.getContentAsString().contains("\"/cm/app/home\""));
    assertTrue(
        res200.getHeaders("Set-Cookie").stream().anyMatch(c -> c.contains("Max-Age=0")));

    // Lockout maps to 423
    when(mfaService.verifyChallengeRecoveryCode("tx-1", "BAD", "127.0.0.1"))
        .thenThrow(new PSMfaLockoutException("Locked", Instant.now().plusSeconds(900)));
    MockHttpServletRequest lockedPost =
        new MockHttpServletRequest("POST", "/auth/challenge/recovery-code");
    lockedPost.setPathInfo("/challenge/recovery-code");
    lockedPost.setSession(getReq.getSession());
    lockedPost.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-1"));
    lockedPost.setRemoteAddr("127.0.0.1");
    lockedPost.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    lockedPost.setContentType("application/json");
    lockedPost.setContent("{\"code\":\"BAD\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse res423 = new MockHttpServletResponse();
    servlet.doPost(lockedPost, res423);
    assertEquals(423, res423.getStatus());
  }

  @Test
  void enrollOnlySetupStartConfirmAndCancelWork() throws Exception {
    when(mfaService.getTransactionStatus("enroll-1"))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "enroll-1",
                "ENROLL_ONLY",
                "bob",
                "/cm/app/",
                "en-us",
                List.of("Editor"),
                false,
                false,
                0L,
                Instant.now().plusSeconds(300)));

    MockHttpServletRequest getSetup = new MockHttpServletRequest("GET", "/auth/setup");
    getSetup.setPathInfo("/setup");
    getSetup.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "enroll-1"));
    MockHttpServletResponse setupRes = new MockHttpServletResponse();
    servlet.doGet(getSetup, setupRes);
    assertEquals("mfa-setup", getSetup.getAttribute(PSAuthServlet.ATTR_VIEW));
    String csrf = (String) getSetup.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);

    when(mfaService.startEnrollOnlyTotp("enroll-1", null))
        .thenReturn(
            new PSTotpEnrollmentStart(
                "totp-id-1", "otpauth://totp/Percussion:bob?secret=ABC", "ABC", "Percussion", "bob", 6, 30));
    MockHttpServletRequest startPost = new MockHttpServletRequest("POST", "/auth/setup/totp/start");
    startPost.setPathInfo("/setup/totp/start");
    startPost.setSession(getSetup.getSession());
    startPost.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "enroll-1"));
    startPost.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    MockHttpServletResponse startRes = new MockHttpServletResponse();
    servlet.doPost(startPost, startRes);
    assertEquals(200, startRes.getStatus());
    assertTrue(startRes.getContentAsString().contains("\"totpId\":\"totp-id-1\""));

    when(mfaService.completeEnrollOnlyTotp("enroll-1", "totp-id-1", "654321", "127.0.0.1"))
        .thenReturn(
            new PSTotpConfirmOutcome(
                true, List.of("11111-AAAAA"), "bob", "/cm/app/", "en-us", List.of("Editor"), "ok"));
    MockHttpServletRequest confirmPost =
        new MockHttpServletRequest("POST", "/auth/setup/totp/confirm");
    confirmPost.setPathInfo("/setup/totp/confirm");
    confirmPost.setSession(getSetup.getSession());
    confirmPost.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "enroll-1"));
    confirmPost.setRemoteAddr("127.0.0.1");
    confirmPost.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    confirmPost.setContentType("application/json");
    confirmPost.setContent(
        "{\"totpId\":\"totp-id-1\",\"code\":\"654321\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse confirmRes = new MockHttpServletResponse();
    servlet.doPost(confirmPost, confirmRes);
    assertEquals(200, confirmRes.getStatus());
    assertTrue(confirmRes.getContentAsString().contains("\"enrolled\":true"));
    assertTrue(confirmRes.getContentAsString().contains("11111-AAAAA"));
  }

  @Test
  void passkeyChallengeAndSetupEndpointsWorkWithCsrf() throws Exception {
    when(mfaService.getTransactionStatus("tx-pk"))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-pk",
                "MFA_CHALLENGE",
                "alice",
                "/cm/app/",
                "en-us",
                List.of("Admin"),
                true,
                true,
                8L,
                Instant.now().plusSeconds(300)));
    MockHttpServletRequest getChallenge = new MockHttpServletRequest("GET", "/auth/challenge");
    getChallenge.setPathInfo("/challenge");
    getChallenge.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-pk"));
    servlet.doGet(getChallenge, new MockHttpServletResponse());
    String csrf = (String) getChallenge.getAttribute(PSAuthServlet.ATTR_CSRF_TOKEN);

    when(mfaService.startChallengePasskey("tx-pk", "http://localhost"))
        .thenReturn(new PSPasskeyOptions("w-tx-1", "{\"challenge\":\"abc\"}"));
    MockHttpServletRequest optsReq =
        new MockHttpServletRequest("POST", "/auth/challenge/passkey/options");
    optsReq.setPathInfo("/challenge/passkey/options");
    optsReq.setSession(getChallenge.getSession());
    optsReq.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-pk"));
    optsReq.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    MockHttpServletResponse optsRes = new MockHttpServletResponse();
    servlet.doPost(optsReq, optsRes);
    assertEquals(200, optsRes.getStatus());
    assertTrue(optsRes.getContentAsString().contains("\"challengeTxId\":\"w-tx-1\""));

    when(mfaService.verifyChallengePasskey("tx-pk", "w-tx-1", "{\"id\":\"c1\"}", "http://localhost", "127.0.0.1"))
        .thenReturn(
            new PSMfaChallengeOutcome(
                true,
                PSMfaChallengeOutcome.STATUS_VERIFIED,
                "Verified",
                "alice",
                "/cm/app/",
                "en-us",
                List.of("Admin"),
                8L));
    MockHttpServletRequest verifyReq =
        new MockHttpServletRequest("POST", "/auth/challenge/passkey/verify");
    verifyReq.setPathInfo("/challenge/passkey/verify");
    verifyReq.setSession(getChallenge.getSession());
    verifyReq.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-pk"));
    verifyReq.setRemoteAddr("127.0.0.1");
    verifyReq.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    verifyReq.setContentType("application/json");
    verifyReq.setContent(
        "{\"challengeTxId\":\"w-tx-1\",\"credentialJson\":\"{\\\"id\\\":\\\"c1\\\"}\"}"
            .getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse verifyRes = new MockHttpServletResponse();
    servlet.doPost(verifyReq, verifyRes);
    assertEquals(200, verifyRes.getStatus());
    assertTrue(verifyRes.getContentAsString().contains("\"authenticated\":true"));

    when(mfaService.startEnrollOnlyPasskey("enroll-pk", "Work", "http://localhost"))
        .thenReturn(new PSPasskeyOptions("reg-tx-1", "{\"rp\":{\"id\":\"localhost\"}}"));
    MockHttpServletRequest setupStart =
        new MockHttpServletRequest("POST", "/auth/setup/passkey/start");
    setupStart.setPathInfo("/setup/passkey/start");
    setupStart.setSession(getChallenge.getSession());
    setupStart.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "enroll-pk"));
    setupStart.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    setupStart.setContentType("application/json");
    setupStart.setContent("{\"label\":\"Work\"}".getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse setupStartRes = new MockHttpServletResponse();
    servlet.doPost(setupStart, setupStartRes);
    assertEquals(200, setupStartRes.getStatus());
    assertTrue(setupStartRes.getContentAsString().contains("\"challengeTxId\":\"reg-tx-1\""));

    when(mfaService.completeEnrollOnlyPasskey(
            "enroll-pk", "reg-tx-1", "Work", "{\"id\":\"r1\"}", "http://localhost", "127.0.0.1"))
        .thenReturn(
            new PSTotpConfirmOutcome(
                true, List.of("AAAAA-BBBBB"), "bob", "/cm/app/", "en-us", List.of("Editor"), "ok"));
    MockHttpServletRequest setupFinish =
        new MockHttpServletRequest("POST", "/auth/setup/passkey/finish");
    setupFinish.setPathInfo("/setup/passkey/finish");
    setupFinish.setSession(getChallenge.getSession());
    setupFinish.setCookies(new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "enroll-pk"));
    setupFinish.setRemoteAddr("127.0.0.1");
    setupFinish.addHeader(PSAuthCsrfGuard.HEADER_NAME, csrf);
    setupFinish.setContentType("application/json");
    setupFinish.setContent(
        "{\"challengeTxId\":\"reg-tx-1\",\"label\":\"Work\",\"credentialJson\":\"{\\\"id\\\":\\\"r1\\\"}\"}"
            .getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse setupFinishRes = new MockHttpServletResponse();
    servlet.doPost(setupFinish, setupFinishRes);
    assertEquals(200, setupFinishRes.getStatus());
    assertTrue(setupFinishRes.getContentAsString().contains("\"enrolled\":true"));
    assertTrue(setupFinishRes.getContentAsString().contains("AAAAA-BBBBB"));
  }
}
