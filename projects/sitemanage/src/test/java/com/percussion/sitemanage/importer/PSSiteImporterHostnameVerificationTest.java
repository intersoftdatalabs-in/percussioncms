/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.sitemanage.importer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSSiteImporter#overrideConnectionProperties()} hostname verification
 * (CodeQL {@code java/unsafe-hostname-verification} alert #663, T053).
 *
 * <p>Pre-fix code installed {@code (host, session) -> true}, which disables RFC 2818 hostname
 * matching. The fix leaves the JVM default {@code HostnameVerifier} in place while still
 * installing default trust managers for certificate validation.
 *
 * <p>HttpsURLConnection defaults are JVM-wide statics; this test captures/restores them in
 * {@code @BeforeEach}/{@code @AfterEach} and installs a unique sentinel verifier so the assertion
 * is not order-dependent under parallel Surefire workers.
 */
@DisplayName("PSSiteImporter.overrideConnectionProperties — hostname verification (T053)")
class PSSiteImporterHostnameVerificationTest {

  private SSLSocketFactory savedSocketFactory;
  private HostnameVerifier savedHostnameVerifier;

  @BeforeEach
  void captureGlobalSslDefaults() {
    savedSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    savedHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
  }

  @AfterEach
  void restoreGlobalSslDefaults() {
    HttpsURLConnection.setDefaultSSLSocketFactory(savedSocketFactory);
    HttpsURLConnection.setDefaultHostnameVerifier(savedHostnameVerifier);
  }

  @Test
  @DisplayName("override does not replace the HostnameVerifier with always-true")
  void overrideDoesNotInstallAlwaysTrueHostnameVerifier() {
    // Unique sentinel instance: if override installed (h,s)->true, assertSame would fail.
    HostnameVerifier sentinel = (hostname, session) -> false;
    HttpsURLConnection.setDefaultHostnameVerifier(sentinel);

    var props = PSSiteImporter.overrideConnectionProperties();
    assertNotNull(props, "overrideConnectionProperties must succeed with default trust managers");
    try {
      assertSame(
          sentinel,
          HttpsURLConnection.getDefaultHostnameVerifier(),
          "override must leave the pre-call HostnameVerifier installed"
              + " (must not replace it with an always-true verifier)");
      assertSame(
          sentinel,
          props.getDefaultHostnameVerifier(),
          "saved properties must record the pre-call HostnameVerifier for restore");
    } finally {
      PSSiteImporter.restoreConnectionProperties(props);
    }

    assertSame(
        sentinel,
        HttpsURLConnection.getDefaultHostnameVerifier(),
        "restoreConnectionProperties must put the pre-override verifier back");
  }
}
