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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.utils.jdbc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Live H2 probes for {@link PSJdbcConnectionDiagnostics} (optional if H2 is on the test CP). */
class PSJdbcConnectionDiagnosticsTest {

  @Test
  void urlContainsNonKeywords_detectsFlag() {
    assertTrue(
        PSJdbcConnectionDiagnostics.urlContainsNonKeywords(
            "jdbc:h2:file:/opt/x;NON_KEYWORDS=VALUE"));
    assertFalse(PSJdbcConnectionDiagnostics.urlContainsNonKeywords("jdbc:h2:file:/opt/x"));
    assertFalse(PSJdbcConnectionDiagnostics.urlContainsNonKeywords(null));
  }

  @Test
  void describeConnection_nullSafe() {
    assertTrue(PSJdbcConnectionDiagnostics.describeConnection(null).contains("null"));
  }

  @Test
  void withNonKeywords_valueProbeSucceeds_andSettingsVisible() throws Exception {
    assumeH2Driver();
    String url = "jdbc:h2:mem:diag_nk;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE";
    try (Connection c = DriverManager.getConnection(url, "sa", "")) {
      // H2 strips settings from getURL — diagnostics must still surface NON_KEYWORDS
      String meta = c.getMetaData().getURL();
      assertFalse(
          PSJdbcConnectionDiagnostics.urlContainsNonKeywords(meta),
          "precondition: H2 getURL strips NON_KEYWORDS (got " + meta + ")");

      String diag = PSJdbcConnectionDiagnostics.describeConnection(c);
      assertNotNull(diag);
      assertTrue(diag.contains("h2.NON_KEYWORDS=VALUE"), diag);
      assertTrue(diag.contains("h2.unquotedVALUE_identifier_ok=true"), diag);
      assertTrue(PSJdbcConnectionDiagnostics.probeUnquotedValueIdentifier(c));
      assertTrue(
          "VALUE".equalsIgnoreCase(PSJdbcConnectionDiagnostics.queryH2Setting(c, "NON_KEYWORDS")));
    }
  }

  @Test
  void withoutNonKeywords_valueProbeFails() throws Exception {
    assumeH2Driver();
    String url = "jdbc:h2:mem:diag_no_nk;DB_CLOSE_DELAY=-1";
    try (Connection c = DriverManager.getConnection(url, "sa", "")) {
      String diag = PSJdbcConnectionDiagnostics.describeConnection(c);
      assertTrue(
          diag.contains("h2.NON_KEYWORDS=<absent>") || diag.contains("h2.NON_KEYWORDS="), diag);
      // absent or empty
      String nk = PSJdbcConnectionDiagnostics.queryH2Setting(c, "NON_KEYWORDS");
      assertTrue(nk == null || nk.isBlank(), "expected no NON_KEYWORDS, got " + nk);
      assertFalse(PSJdbcConnectionDiagnostics.probeUnquotedValueIdentifier(c));
      assertTrue(diag.contains("h2.unquotedVALUE_identifier_ok=false"), diag);
    }
  }

  private static void assumeH2Driver() {
    try {
      Class.forName("org.h2.Driver");
    } catch (ClassNotFoundException e) {
      Assumptions.assumeTrue(false, "org.h2.Driver not on test classpath");
    }
  }
}
