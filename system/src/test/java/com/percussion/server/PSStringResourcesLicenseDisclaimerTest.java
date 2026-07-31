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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the startup license disclaimer strings printed by {@link PSServer#init} (see issue
 * #1529) reflect the third-party components actually bundled with the current build, and no longer
 * reference stale versions or components that are no longer shipped.
 */
public class PSStringResourcesLicenseDisclaimerTest {

  private ResourceBundle serverBundle;

  @BeforeEach
  void setUp() {
    serverBundle = ResourceBundle.getBundle("com.percussion.server.PSStringResources");
  }

  @Test
  void copyrightKeyIsPresentAndNotBlank() {
    String copyright = serverBundle.getString("copyright");
    assertTrue(StringUtils.isNotBlank(copyright), "copyright resource must not be blank");
    assertTrue(copyright.contains("Percussion"), "copyright resource must mention Percussion");
  }

  @Test
  void thirdPartyCopyrightKeyIsPresentAndNotBlank() {
    String thirdParty = serverBundle.getString("thirdPartyCopyright");
    assertTrue(
        StringUtils.isNotBlank(thirdParty), "thirdPartyCopyright resource must not be blank");
  }

  @Test
  void thirdPartyCopyrightReferencesCurrentlyBundledComponents() {
    String thirdParty = serverBundle.getString("thirdPartyCopyright");

    // Apache Software Foundation blanket attribution is still accurate (Commons libraries, etc.)
    assertTrue(
        thirdParty.contains("Apache Software Foundation"),
        "disclaimer must credit the Apache Software Foundation");

    // Server now runs on Eclipse Jetty, not Tomcat - the disclaimer must say so.
    // (Intentionally no version pin in the user-authored text: Eclipse Jetty's own attribution uses
    // "Eclipse Foundation and other contributors" without a numeric version tag. Trust upstream
    // attribution wording rather than deriving from the artifact version.)
    assertTrue(thirdParty.contains("Jetty"), "disclaimer must credit Eclipse Jetty");
    assertTrue(
        thirdParty.contains("Eclipse Foundation"),
        "disclaimer must use Eclipse Foundation's upstream copyright holder wording");

    // jTDS is bundled at 1.3.1 per root pom.xml jtds.version.
    assertTrue(thirdParty.contains("jTDS"), "disclaimer must credit the jTDS driver");
    assertTrue(thirdParty.contains("v1.3.1"), "disclaimer must reference the current jTDS version");

    // The Microsoft JDBC driver for SQL Server is now also bundled per root pom.xml
    // mssql.version. Intentionally no version pin: MIT-licensed upstream uses a generic name.
    assertTrue(
        thirdParty.contains("Microsoft JDBC Driver for SQL Server"),
        "disclaimer must credit the Microsoft JDBC Driver for SQL Server");
    assertTrue(
        thirdParty.contains("MIT"),
        "disclaimer must declare the Microsoft JDBC Driver license (MIT)");

    // XStream is pinned via root pom.xml xstream.version. The disclaimer text follows the upstream
    // BSD-style attribution form, which uses year ranges and a copyright holder rather than a
    // numeric version pin. Assert the upstream wording instead of the artifact version.
    assertTrue(thirdParty.contains("XStream"), "disclaimer must credit XStream");
    assertTrue(
        thirdParty.contains("XStream Committers"),
        "disclaimer must use the current XStream Committers copyright holder");
    assertTrue(
        thirdParty.contains("2006-2024"),
        "disclaimer must reflect the current XStream copyright year range (2006-2024)");

    // ASM is pinned via root pom.xml asm dependency management; verify current copyright form.
    // ASM uses a copyright year range in its BSD-3 attribution (per upstream LICENSE), not a
    // numeric version pin; do NOT derive it from the artifact version (root pom declares asm=9.9.1).
    assertTrue(thirdParty.contains("ASM"), "disclaimer must credit ASM");
    assertTrue(
        thirdParty.contains("2000-2011"), "disclaimer must use ASM's upstream copyright year range");
  }

  @Test
  void thirdPartyCopyrightDoesNotReferenceStaleOrRemovedComponents() {
    String thirdParty = serverBundle.getString("thirdPartyCopyright");

    // Old jTDS version string must be gone.
    assertFalse(thirdParty.contains("v1.2.2"), "stale jTDS version v1.2.2 must be removed");

    // Stale ASM copyright year range must be gone.
    assertFalse(thirdParty.contains("2000-2005"), "stale ASM copyright years must be removed");

    // Stale XStream copyright year range must be gone.
    assertFalse(thirdParty.contains("2003-2005"), "stale XStream copyright years must be removed");

    // Lato font is not bundled with this build; its clause (and the SIL Open Font License
    // reference) must be removed.
    assertFalse(thirdParty.contains("Lato"), "Lato font clause must be removed - not bundled");
    assertFalse(
        thirdParty.contains("SIL Open Font License"),
        "SIL Open Font License clause must be removed - Lato is not bundled");

    // The old, unsubstantiated "GNU Runtime Libraries" blanket clause must be removed.
    assertFalse(
        thirdParty.contains("GNU Runtime Libraries"),
        "unsubstantiated GNU Runtime Libraries clause must be removed");
  }
}
