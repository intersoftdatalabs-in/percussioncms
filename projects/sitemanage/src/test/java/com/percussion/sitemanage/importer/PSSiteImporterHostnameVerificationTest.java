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

import javax.net.ssl.HttpsURLConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSSiteImporter#overrideConnectionProperties()} hostname verification
 * (CodeQL {@code java/unsafe-hostname-verification} alert #663, T053).
 *
 * <p>Pre-fix code installed {@code (host, session) -> true}, which disables RFC 2818 hostname
 * matching. The fix leaves the JVM default {@code HostnameVerifier} in place while still
 * installing default trust managers for certificate validation.
 */
@DisplayName("PSSiteImporter.overrideConnectionProperties — hostname verification (T053)")
class PSSiteImporterHostnameVerificationTest {

  @Test
  @DisplayName("override does not replace the default HostnameVerifier with always-true")
  void overrideKeepsDefaultHostnameVerifier() {
    var props = PSSiteImporter.overrideConnectionProperties();
    assertNotNull(props, "overrideConnectionProperties must succeed with default trust managers");
    try {
      // After override the active verifier must still be the one that was default before the
      // call (saved on the properties object). An always-true lambda would be a different
      // instance and would re-open CWE-295 / CodeQL #663.
      assertSame(
          props.getDefaultHostnameVerifier(),
          HttpsURLConnection.getDefaultHostnameVerifier(),
          "override must not install a custom always-true HostnameVerifier");
    } finally {
      PSSiteImporter.restoreConnectionProperties(props);
    }
  }
}
