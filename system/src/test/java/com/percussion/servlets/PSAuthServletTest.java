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
import com.percussion.services.auth.ratelimit.PSAuthRateLimitException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag("UnitTest")
class PSAuthServletTest {

  private IPSAccountSecurityService accountService;
  private PSAuthCsrfGuard csrfGuard;
  private PSAuthServlet servlet;

  @BeforeEach
  void setUp() {
    accountService = mock(IPSAccountSecurityService.class);
    csrfGuard = new PSAuthCsrfGuard(Properties::new);
    servlet = new PSAuthServlet(accountService, csrfGuard);
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
}
