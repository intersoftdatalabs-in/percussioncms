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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Regression guard: the DTS shipping jar must include the full Cargo-packaged Tomcat tree (not only
 * {@code Deployment/Server/common/lib} logging jars).
 *
 * <p>Without this, {@code java -jar delivery-tier-distribution.jar} can report BUILD SUCCESSFUL in
 * a few seconds while leaving no {@code conf/server.xml}, {@code bin/}, or DTS wars — the
 * hollow-payload failure mode after #1471/#1473/#1475 preinstall work.
 *
 * <p>No-op when {@code target/delivery-tier-distribution.jar} is absent (IDE / ad-hoc runs).
 */
class DtsInstallerJarContainsTomcatTreeTest {

  private static final String SERVER_PREFIX = "distribution/Deployment/Server/";

  /** Cargo {@code deployables} in pom.xml — all must ship or a service is silently missing. */
  private static final String[] DTS_WARS = {
    "perc-metadata-services.war",
    "perc-comments-services.war",
    "perc-form-processor.war",
    "perc-membership-services.war",
    "perc-polls-services.war",
    "feeds.war",
    "perc-common-ui.war",
  };

  @Test
  void shippingJarBundlesTomcatServerTreeAndDtsWars() throws IOException {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    List<String> entries = new ArrayList<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream().map(java.util.zip.ZipEntry::getName).forEach(entries::add);
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }

    assertContains(entries, SERVER_PREFIX + "conf/server.xml");

    // Dual-platform console launchers from the Cargo Tomcat package
    assertContains(entries, SERVER_PREFIX + "bin/catalina.sh");
    assertContains(entries, SERVER_PREFIX + "bin/catalina.bat");

    // Windows Procrun / service binaries ship in rootFiles (install root of the jar
    // payload). installDts.xml later copies tomcat11.exe into Server/bin on Windows.
    assertContains(entries, "distribution/tomcat11.exe");
    assertContains(entries, "distribution/tomcat11w.exe");

    for (String war : DTS_WARS) {
      assertContains(entries, SERVER_PREFIX + "webapps/" + war);
    }

    // Manager apps must remain stripped from the shipping payload
    assertTrue(
        entries.stream().noneMatch(n -> n.startsWith(SERVER_PREFIX + "webapps/manager/")),
        "shipping jar must not include Tomcat manager webapp");
    assertTrue(
        entries.stream().noneMatch(n -> n.startsWith(SERVER_PREFIX + "webapps/host-manager/")),
        "shipping jar must not include Tomcat host-manager webapp");
  }

  private static void assertContains(List<String> entries, String required) {
    assertTrue(
        entries.contains(required),
        () ->
            "Expected entry "
                + required
                + " in shipping jar; Cargo package was not staged into"
                + " distribution/Deployment/Server before jar packaging."
                + " See antrun id=stage-cargo-package-into-assembly.");
  }
}
