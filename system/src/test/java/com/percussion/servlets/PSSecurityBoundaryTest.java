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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.security.PSRoleEntry;
import com.percussion.security.PSUserEntry;
import com.percussion.services.auth.data.PSAuthLoginTx;
import com.percussion.services.auth.mfa.IPSMfaService;
import com.percussion.services.auth.mfa.PSMfaTxStatus;
import com.percussion.services.auth.policy.IPSAuthPolicyService;
import com.percussion.services.auth.policy.PSLoginPolicyDecision;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfoBase;
import com.percussion.utils.server.IPSCgiVariables;
import com.percussion.webservices.security.impl.PSSecurityWs;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PSSecurityBoundaryTest {

  private static final String SYSTEM_SECURITY_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <securityConfiguration>
        <path authType="basic">/Design*</path>
        <path authType="anonymous">/rxauth.jsp</path>
        <path authType="anonymous">/auth/*</path>
      </securityConfiguration>
      """;

  @TempDir Path tempWebRoot;

  @AfterEach
  void tearDown() {
    PSSecurityFilter.resetAuthHooksForTest();
    if (PSRequestInfoBase.isInited()) {
      PSRequestInfoBase.resetRequestInfo();
    }
  }

  private PSSecurityFilter initTestFilter() throws Exception {
    Path secDir = tempWebRoot.resolve("WEB-INF").resolve("config").resolve("security");
    Path userSecDir =
        tempWebRoot.resolve("WEB-INF").resolve("config").resolve("user").resolve("security");
    Files.createDirectories(secDir);
    Files.createDirectories(userSecDir);
    Files.writeString(secDir.resolve("system-security-conf.xml"), SYSTEM_SECURITY_XML);
    Files.writeString(
        userSecDir.resolve("user-security-conf.xml"),
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<securityConfiguration forceSecureLogin=\"no\"></securityConfiguration>\n");
    PSSecurityFilter filter = new PSSecurityFilter();
    filter.initSecurityConfiguration(tempWebRoot.toString());
    return filter;
  }

  private static Subject subjectFor(String user, String... roles) {
    PSRoleEntry[] entries = new PSRoleEntry[roles.length];
    for (int i = 0; i < roles.length; i++) {
      entries[i] = new PSRoleEntry(roles[i], 0);
    }
    return PSJaasUtils.userEntryToSubject(new PSUserEntry(user, 0, null, entries, null, null), null);
  }

  @Test
  void pendingMfaChallengeDoesNotGrantSessionOrAuthenticatedSubject() throws Exception {
    Subject alice = subjectFor("alice", "Admin");
    IPSMfaService mfa = mock(IPSMfaService.class);
    when(mfa.createLoginTransaction(
            eq("MFA_CHALLENGE"), eq("alice"), eq("/cm/app/home"), eq("en-us"), anyCollection()))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-opaque-1",
                "MFA_CHALLENGE",
                "alice",
                "/cm/app/home",
                "en-us",
                List.of("Admin"),
                true,
                false,
                10L,
                Instant.now().plusSeconds(300)));
    PSSecurityFilter.mfaServiceOverride = mfa;

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
    req.setScheme("https");
    req.setServerName("cms.example.com");
    req.setServerPort(443);
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSSecurityFilter.beginMfaLoginChallenge(
        req, res, alice, PSLoginPolicyDecision.REQUIRE_MFA_CHALLENGE, "/cm/app/home", "en-us");

    assertNull(req.getSession().getAttribute(PSSecurityFilter.SUBJECT));
    assertFalse(PSSecurityFilter.isAuthenticated(req));
    assertNotNull(req.getSession().getAttribute(PSSecurityFilter.ATTR_PENDING_MFA_TX));
    assertEquals("/auth/challenge", res.getRedirectedUrl());
    List<String> setCookies = res.getHeaders("Set-Cookie");
    assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("PERC_AUTH_TX=tx-opaque-1;")));
    assertFalse(setCookies.stream().anyMatch(c -> c.startsWith("pssessionid=")));
  }

  @Test
  void finalizeSessionRotatesJsessionIdAndStampsSessionVersionAndBlocksStaleVersion()
      throws Exception {
    Subject alice = subjectFor("alice", "Admin");
    PSSecurityFilter.sessionVersionResolver = user -> 1L;

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/challenge/totp");
    req.setScheme("https");
    req.setServerName("cms.example.com");
    req.setServerPort(443);
    HttpSession before = req.getSession(true);
    String initialSessionId = before.getId();
    before.setAttribute(
        PSSecurityFilter.ATTR_PENDING_MFA_TX,
        new PSSecurityFilter.PendingMfaContext("tx-1", "alice", alice));
    MockHttpServletResponse res = new MockHttpServletResponse();

    PSSecurityFilter.finalizeAuthenticatedSession(req, res, alice, "fr-fr");

    assertNotEquals(initialSessionId, req.getSession(false).getId());
    assertNull(req.getSession(false).getAttribute(PSSecurityFilter.ATTR_PENDING_MFA_TX));
    assertEquals(1L, req.getSession(false).getAttribute(PSSecurityFilter.ATTR_SESSION_VERSION));
    assertTrue(PSSecurityFilter.isAuthenticated(req));
    assertTrue(res.getHeaders("Set-Cookie").stream().anyMatch(c -> c.startsWith("pssessionid=")));

    if (PSRequestInfoBase.isInited()) {
      PSRequestInfoBase.resetRequestInfo();
    }

    // Simulate password reset bumping DB SESSION_VERSION -> stale HTTP session is invalidated!
    PSSecurityFilter.sessionVersionResolver = user -> 2L;
    PSSecurityFilter filter = initTestFilter();
    MockHttpServletRequest protectedReq = new MockHttpServletRequest("GET", "/cm/app/home");
    protectedReq.setServletPath("/cm/app/home");
    protectedReq.setSession(req.getSession(false));
    MockHttpServletResponse protectedRes = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(protectedReq, protectedRes, chain);

    assertTrue(
        protectedRes.getRedirectedUrl() != null
            && protectedRes.getRedirectedUrl().contains("/login"));
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void basicAuthHeaderOverrideBlockedForMfaUserAndAllowedForServiceAccount() throws Exception {
    Subject alice = subjectFor("alice", "Editor");
    Subject rxserver = subjectFor("rxserver", "Admin");
    PSSecurityFilter.loginVerifierOverride =
        (req, uid, pwd) -> "rxserver".equals(uid) ? rxserver : alice;

    IPSAuthPolicyService policy = mock(IPSAuthPolicyService.class);
    when(policy.isNonInteractivePasswordChannelAllowed(
            eq("alice"), anyCollection(), eq("HEADER_BASIC_AUTH")))
        .thenReturn(false);
    when(policy.isNonInteractivePasswordChannelAllowed(
            eq("rxserver"), anyCollection(), eq("HEADER_BASIC_AUTH")))
        .thenReturn(true);
    when(policy.isNonInteractivePasswordChannelAllowed(
            eq("alice"), anyCollection(), eq("SOAP_WS")))
        .thenReturn(false);
    PSSecurityFilter.authPolicyServiceOverride = policy;

    PSSecurityFilter filter = initTestFilter();

    MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/cm/app/home");
    blockedReq.setServletPath("/cm/app/home");
    blockedReq.addHeader(IPSHtmlParameters.SYS_USE_BASIC_AUTH, "true");
    blockedReq.addHeader(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("alice:pw".getBytes(StandardCharsets.UTF_8)));
    MockHttpServletResponse blockedRes = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(blockedReq, blockedRes, chain);

    assertEquals(403, blockedRes.getStatus());
    assertFalse(PSSecurityFilter.isAuthenticated(blockedReq));
    verify(chain, never()).doFilter(any(), any());

    // Service user succeeds with same header override
    MockHttpServletRequest svcReq = new MockHttpServletRequest("GET", "/cm/app/home");
    svcReq.setServletPath("/cm/app/home");
    svcReq.addHeader(IPSHtmlParameters.SYS_USE_BASIC_AUTH, "true");
    svcReq.addHeader(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("rxserver:pw".getBytes(StandardCharsets.UTF_8)));
    MockHttpServletResponse svcRes = new MockHttpServletResponse();
    FilterChain svcChain = mock(FilterChain.class);

    filter.doFilter(svcReq, svcRes, svcChain);
    verify(svcChain).doFilter(any(), any());

    // SOAP login blocked when policy disallows non-interactive password channel
    PSSecurityWs ws = new PSSecurityWs();
    MockHttpServletRequest soapReq = new MockHttpServletRequest();
    MockHttpServletResponse soapRes = new MockHttpServletResponse();
    assertThrows(
        LoginException.class,
        () -> ws.login(soapReq, soapRes, "alice", "pw", "client", "", ""));
  }

  @Test
  void csrfOriginCheckStillBlocksCrossOriginWhenBehindReverseProxy() throws Exception {
    PSSecurityFilter filter = initTestFilter();
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/rest/content");
    req.setServletPath("/rest/content");
    req.setScheme("http");
    req.setServerName("127.0.0.1");
    req.setServerPort(9992);
    req.addHeader("Origin", "https://evil.example.com");
    req.addHeader("X-Forwarded-Host", "cms.example.com");
    Subject alice = subjectFor("alice", "Admin");
    req.getSession(true).setAttribute(PSSecurityFilter.SUBJECT, alice);
    req.getSession(false).setAttribute(PSSecurityFilter.ATTR_SESSION_VERSION, 1L);

    MockHttpServletResponse res = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(req, res, chain);
    assertEquals(302, res.getStatus());
    assertTrue(res.getRedirectedUrl() != null && res.getRedirectedUrl().contains("/ui/error.jsp"));
    verify(chain, never()).doFilter(any(), any());
  }

  @Test
  void pendingMfaChallengeTxCannotAccessProtectedUiRestOrLegacyRoutes() throws Exception {
    Subject alice = subjectFor("alice", "Admin");
    IPSMfaService mfa = mock(IPSMfaService.class);
    when(mfa.createLoginTransaction(
            eq(PSAuthLoginTx.TYPE_MFA_CHALLENGE),
            eq("alice"),
            eq("/cm/app/home"),
            eq("en-us"),
            anyCollection()))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-challenge-only",
                PSAuthLoginTx.TYPE_MFA_CHALLENGE,
                "alice",
                "/cm/app/home",
                "en-us",
                List.of("Admin"),
                true,
                false,
                10L,
                Instant.now().plusSeconds(300)));
    PSSecurityFilter.mfaServiceOverride = mfa;

    MockHttpServletRequest loginReq = new MockHttpServletRequest("POST", "/login");
    loginReq.setScheme("https");
    loginReq.setServerName("cms.example.com");
    loginReq.setServerPort(443);
    MockHttpServletResponse loginRes = new MockHttpServletResponse();
    PSSecurityFilter.beginMfaLoginChallenge(
        loginReq,
        loginRes,
        alice,
        PSLoginPolicyDecision.REQUIRE_MFA_CHALLENGE,
        "/cm/app/home",
        "en-us");

    HttpSession pendingSession = loginReq.getSession(false);
    Cookie txCookie = new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-challenge-only");
    PSSecurityFilter filter = initTestFilter();

    for (String protectedPath :
        List.of("/cm/app/home", "/rest/content", "/services/auth/mfa/status")) {
      if (PSRequestInfoBase.isInited()) {
        PSRequestInfoBase.resetRequestInfo();
      }
      MockHttpServletRequest req = new MockHttpServletRequest("GET", protectedPath);
      req.setServletPath(protectedPath);
      req.setSession(pendingSession);
      req.setCookies(txCookie);
      MockHttpServletResponse res = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);

      filter.doFilter(req, res, chain);
      assertFalse(PSSecurityFilter.isAuthenticated(req));
      assertTrue(res.getRedirectedUrl() != null && res.getRedirectedUrl().contains("/login"));
      verify(chain, never()).doFilter(any(), any());
    }

    if (PSRequestInfoBase.isInited()) {
      PSRequestInfoBase.resetRequestInfo();
    }
    MockHttpServletRequest legacyReq = new MockHttpServletRequest("GET", "/Design");
    legacyReq.setServletPath("/Design");
    legacyReq.setSession(pendingSession);
    legacyReq.setCookies(txCookie);
    MockHttpServletResponse legacyRes = new MockHttpServletResponse();
    FilterChain legacyChain = mock(FilterChain.class);

    filter.doFilter(legacyReq, legacyRes, legacyChain);
    assertEquals(401, legacyRes.getStatus());
    assertFalse(PSSecurityFilter.isAuthenticated(legacyReq));
    verify(legacyChain, never()).doFilter(any(), any());
  }

  @Test
  void pendingEnrollOnlyTxRedirectsToSetupAndCannotAccessProtectedRoutes() throws Exception {
    Subject alice = subjectFor("alice", "Editor");
    IPSMfaService mfa = mock(IPSMfaService.class);
    when(mfa.createLoginTransaction(
            eq(PSAuthLoginTx.TYPE_MFA_ENROLL),
            eq("alice"),
            eq("/cm/app/home"),
            eq("en-us"),
            anyCollection()))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-enroll-only",
                PSAuthLoginTx.TYPE_MFA_ENROLL,
                "alice",
                "/cm/app/home",
                "en-us",
                List.of("Editor"),
                false,
                false,
                10L,
                Instant.now().plusSeconds(300)));
    PSSecurityFilter.mfaServiceOverride = mfa;

    MockHttpServletRequest loginReq = new MockHttpServletRequest("POST", "/login");
    loginReq.setScheme("https");
    loginReq.setServerName("cms.example.com");
    loginReq.setServerPort(443);
    MockHttpServletResponse loginRes = new MockHttpServletResponse();
    PSSecurityFilter.beginMfaLoginChallenge(
        loginReq,
        loginRes,
        alice,
        PSLoginPolicyDecision.REQUIRE_MFA_ENROLLMENT,
        "/cm/app/home",
        "en-us");

    assertNull(loginReq.getSession(false).getAttribute(PSSecurityFilter.SUBJECT));
    assertEquals("/auth/setup", loginRes.getRedirectedUrl());
    List<String> setCookies = loginRes.getHeaders("Set-Cookie");
    assertTrue(setCookies.stream().anyMatch(c -> c.startsWith("PERC_AUTH_TX=tx-enroll-only;")));
    assertFalse(setCookies.stream().anyMatch(c -> c.startsWith("pssessionid=")));

    PSSecurityFilter filter = initTestFilter();
    HttpSession pendingSession = loginReq.getSession(false);
    Cookie txCookie = new Cookie(PSSecurityFilter.AUTH_TX_COOKIE_NAME, "tx-enroll-only");

    for (String protectedPath : List.of("/cm/app/home", "/rest/content")) {
      if (PSRequestInfoBase.isInited()) {
        PSRequestInfoBase.resetRequestInfo();
      }
      MockHttpServletRequest req = new MockHttpServletRequest("GET", protectedPath);
      req.setServletPath(protectedPath);
      req.setSession(pendingSession);
      req.setCookies(txCookie);
      MockHttpServletResponse res = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);

      filter.doFilter(req, res, chain);
      assertFalse(PSSecurityFilter.isAuthenticated(req));
      verify(chain, never()).doFilter(any(), any());
    }
  }

  @Test
  void directBasicAuthOnProtectedBasicRouteBlockedForMfaUserAndAllowedForService()
      throws Exception {
    Subject alice = subjectFor("alice", "Editor");
    Subject rxserver = subjectFor("rxserver", "Admin");
    PSSecurityFilter.loginVerifierOverride =
        (req, uid, pwd) -> "rxserver".equals(uid) ? rxserver : alice;

    IPSAuthPolicyService policy = mock(IPSAuthPolicyService.class);
    when(policy.isNonInteractivePasswordChannelAllowed(
            eq("alice"), anyCollection(), eq("BASIC_AUTH")))
        .thenReturn(false);
    when(policy.isNonInteractivePasswordChannelAllowed(
            eq("rxserver"), anyCollection(), eq("BASIC_AUTH")))
        .thenReturn(true);
    PSSecurityFilter.authPolicyServiceOverride = policy;

    PSSecurityFilter filter = initTestFilter();

    MockHttpServletRequest blockedReq = new MockHttpServletRequest("GET", "/Design");
    blockedReq.setServletPath("/Design");
    blockedReq.addHeader(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("alice:pw".getBytes(StandardCharsets.UTF_8)));
    MockHttpServletResponse blockedRes = new MockHttpServletResponse();
    FilterChain blockedChain = mock(FilterChain.class);

    filter.doFilter(blockedReq, blockedRes, blockedChain);

    assertEquals(403, blockedRes.getStatus());
    assertFalse(PSSecurityFilter.isAuthenticated(blockedReq));
    verify(blockedChain, never()).doFilter(any(), any());

    if (PSRequestInfoBase.isInited()) {
      PSRequestInfoBase.resetRequestInfo();
    }

    MockHttpServletRequest svcReq = new MockHttpServletRequest("GET", "/Design");
    svcReq.setServletPath("/Design");
    svcReq.addHeader(
        "Authorization",
        "Basic "
            + Base64.getEncoder().encodeToString("rxserver:pw".getBytes(StandardCharsets.UTF_8)));
    MockHttpServletResponse svcRes = new MockHttpServletResponse();
    FilterChain svcChain = mock(FilterChain.class);

    filter.doFilter(svcReq, svcRes, svcChain);
    assertTrue(PSSecurityFilter.isAuthenticated(svcReq));
    verify(svcChain).doFilter(any(), any());
  }

  @Test
  void externalSsoHeadersRequireMfaChallengeUnlessExternalMfaPolicyAllowsDirect()
      throws Exception {
    Subject alice = subjectFor("alice", "Editor");
    PSSecurityFilter.loginVerifierOverride = (req, uid, pwd) -> alice;
    PSSecurityFilter.sessionVersionResolver = user -> 1L;

    IPSMfaService mfa = mock(IPSMfaService.class);
    when(mfa.createLoginTransaction(
            eq(PSAuthLoginTx.TYPE_MFA_CHALLENGE),
            eq("alice"),
            eq(null),
            eq(null),
            anyCollection()))
        .thenReturn(
            new PSMfaTxStatus(
                true,
                "tx-sso-challenge",
                PSAuthLoginTx.TYPE_MFA_CHALLENGE,
                "alice",
                null,
                null,
                List.of("Editor"),
                true,
                false,
                10L,
                Instant.now().plusSeconds(300)));
    PSSecurityFilter.mfaServiceOverride = mfa;

    IPSAuthPolicyService policy = mock(IPSAuthPolicyService.class);
    when(policy.evaluateInteractiveLogin(eq("alice"), anyCollection(), eq(true)))
        .thenReturn(PSLoginPolicyDecision.REQUIRE_MFA_CHALLENGE)
        .thenReturn(PSLoginPolicyDecision.ALLOW_DIRECT_SESSION);
    PSSecurityFilter.authPolicyServiceOverride = policy;

    PSSecurityFilter filter = initTestFilter();

    MockHttpServletRequest challengeReq = new MockHttpServletRequest("GET", "/cm/app/home");
    challengeReq.setServletPath("/cm/app/home");
    challengeReq.addHeader(IPSCgiVariables.CGI_AUTH_TYPE, "SSO");
    MockHttpServletResponse challengeRes = new MockHttpServletResponse();
    FilterChain challengeChain = mock(FilterChain.class);

    filter.doFilter(challengeReq, challengeRes, challengeChain);

    assertEquals("/auth/challenge", challengeRes.getRedirectedUrl());
    assertFalse(PSSecurityFilter.isAuthenticated(challengeReq));
    assertTrue(
        challengeRes.getHeaders("Set-Cookie").stream()
            .anyMatch(c -> c.startsWith("PERC_AUTH_TX=tx-sso-challenge;")));
    verify(challengeChain, never()).doFilter(any(), any());

    if (PSRequestInfoBase.isInited()) {
      PSRequestInfoBase.resetRequestInfo();
    }

    MockHttpServletRequest directReq = new MockHttpServletRequest("GET", "/cm/app/home");
    directReq.setServletPath("/cm/app/home");
    directReq.addHeader(IPSCgiVariables.CGI_AUTH_TYPE, "SSO");
    MockHttpServletResponse directRes = new MockHttpServletResponse();
    FilterChain directChain = mock(FilterChain.class);

    filter.doFilter(directReq, directRes, directChain);

    assertTrue(PSSecurityFilter.isAuthenticated(directReq));
    verify(directChain).doFilter(any(), any());
  }

  @Test
  void postWithStaleSessionVersionIsRejectedAndDoesNotInvokeChain() throws Exception {
    Subject alice = subjectFor("alice", "Admin");
    PSSecurityFilter.sessionVersionResolver = user -> 2L;
    PSSecurityFilter filter = initTestFilter();

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/cm/app/save");
    req.setServletPath("/cm/app/save");
    req.setScheme("https");
    req.setServerName("cms.example.com");
    req.setServerPort(443);
    req.addHeader("Origin", "https://cms.example.com");
    req.getSession(true).setAttribute(PSSecurityFilter.SUBJECT, alice);
    req.getSession(false).setAttribute(PSSecurityFilter.ATTR_SESSION_VERSION, 1L);

    MockHttpServletResponse res = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(req, res, chain);

    assertTrue(res.getRedirectedUrl() != null && res.getRedirectedUrl().contains("/ui/error.jsp"));
    verify(chain, never()).doFilter(any(), any());
  }
}
