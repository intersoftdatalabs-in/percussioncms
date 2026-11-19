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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.auth.account.impl.PSAccountSecurityService;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("UnitTest")
class PSAuthCsrfGuardTest {

  @Test
  void validatesMatchingSessionTokenAndCanonicalOriginIncludingReverseProxyPublicBaseUrl() {
    Properties props = new Properties();
    props.setProperty(PSAccountSecurityService.PROP_PUBLIC_BASE_URL, "https://cms.example.com/Rhythmyx");
    PSAuthCsrfGuard guard = new PSAuthCsrfGuard(() -> props);

    MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/auth/verify-email");
    getReq.setScheme("http");
    getReq.setServerName("127.0.0.1");
    getReq.setServerPort(9992);
    String token = guard.ensureSessionToken(getReq);
    assertNotNull(token);
    assertEquals(token, guard.ensureSessionToken(getReq));

    MockHttpServletRequest postReq = new MockHttpServletRequest("POST", "/auth/verify-email");
    postReq.setSession(getReq.getSession());
    postReq.setScheme("http");
    postReq.setServerName("127.0.0.1");
    postReq.setServerPort(9992);
    postReq.addHeader("Origin", "https://cms.example.com");
    postReq.addHeader("Sec-Fetch-Site", "same-origin");
    postReq.addHeader(PSAuthCsrfGuard.HEADER_NAME, token);

    assertTrue(guard.validatePostRequest(postReq));
  }

  @Test
  void rejectsCrossSiteFetchMetadataMismatchedOriginOrBadCsrfToken() {
    PSAuthCsrfGuard guard = new PSAuthCsrfGuard(Properties::new);

    MockHttpServletRequest baseGet = new MockHttpServletRequest("GET", "/auth/forgot-username");
    String token = guard.ensureSessionToken(baseGet);

    MockHttpServletRequest crossSite = new MockHttpServletRequest("POST", "/auth/forgot-username");
    crossSite.setSession(baseGet.getSession());
    crossSite.addHeader("Sec-Fetch-Site", "cross-site");
    crossSite.addHeader(PSAuthCsrfGuard.HEADER_NAME, token);
    assertFalse(guard.validatePostRequest(crossSite));

    MockHttpServletRequest badOrigin = new MockHttpServletRequest("POST", "/auth/forgot-username");
    badOrigin.setSession(baseGet.getSession());
    badOrigin.setScheme("https");
    badOrigin.setServerName("cms.example.com");
    badOrigin.setServerPort(443);
    badOrigin.addHeader("Origin", "https://evil.example.com");
    badOrigin.addHeader(PSAuthCsrfGuard.HEADER_NAME, token);
    assertFalse(guard.validatePostRequest(badOrigin));

    MockHttpServletRequest badCsrf = new MockHttpServletRequest("POST", "/auth/forgot-username");
    badCsrf.setSession(baseGet.getSession());
    badCsrf.setScheme("https");
    badCsrf.setServerName("cms.example.com");
    badCsrf.setServerPort(443);
    badCsrf.addHeader("Origin", "https://cms.example.com");
    badCsrf.addHeader(PSAuthCsrfGuard.HEADER_NAME, "wrong-token");
    assertFalse(guard.validatePostRequest(badCsrf));
  }
}
