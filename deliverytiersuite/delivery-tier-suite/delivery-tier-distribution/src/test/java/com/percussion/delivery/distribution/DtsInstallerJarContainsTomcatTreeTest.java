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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
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

    // #1500 matrix / product default: HTTP on ${http.port} (9980 via perc-catalina.properties),
    // not stock Tomcat 8080. Without PROPERTY_SOURCE + common/lib, digester cannot resolve it.
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      String serverXml = readZipEntry(zip, SERVER_PREFIX + "conf/server.xml");
      assertTrue(
          serverXml.contains("port=\"${http.port}\""),
          "server.xml must bind HTTP to ${http.port} (perc-catalina.properties default 9980);"
              + " stock Tomcat port 8080 breaks matrix probe 9980 and product port docs");
      assertTrue(
          serverXml.contains("port=\"${shutdown.port}\"")
              || serverXml.contains("<Server port=\"${shutdown.port}\""),
          "server.xml must use ${shutdown.port} for Server shutdown port");

      String catalinaProps = readZipEntry(zip, SERVER_PREFIX + "conf/catalina.properties");
      assertTrue(
          catalinaProps.contains(
              "org.apache.tomcat.util.digester.PROPERTY_SOURCE=com.percussion.tomcat.PSTomcatPropertySource"),
          "catalina.properties must register PSTomcatPropertySource for ${http.port} resolution");
      assertTrue(
          catalinaProps.contains("common/lib"),
          "catalina.properties common.loader must include common/lib for perc-tomcat-common");

      // Tomcat 11 HTTPS: nested SSLHostConfig required (legacy Connector keystore* attrs fail).
      assertTrue(
          serverXml.contains("<SSLHostConfig"),
          "server.xml HTTPS connector must nest SSLHostConfig for Tomcat 11");
      assertTrue(
          serverXml.contains("certificateKeystoreFile=\"${https.keystoreFile}\""),
          "server.xml SSLHostConfig Certificate must set certificateKeystoreFile");
      assertTrue(
          !serverXml.matches("(?s).*(?<![A-Za-z])keystoreFile=\"\\$\\{https\\.keystoreFile\\}\".*"),
          "server.xml must not put legacy keystoreFile on the Connector");
    }
  }

  private static String readZipEntry(ZipFile zip, String name) throws IOException {
    ZipEntry entry = zip.getEntry(name);
    if (entry == null) {
      fail("Missing zip entry: " + name);
    }
    try (InputStream in = zip.getInputStream(entry)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
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

  /**
   * Source-tree guard (always runs): Tomcat 11 HTTPS must use nested SSLHostConfig even when the
   * shipping jar has not been rebuilt in this workspace.
   */
  @Test
  void sourceServerXmlNestsSslHostConfigForTomcat11() throws IOException {
    Path serverXml = Path.of("src", "main", "tomcat11", "conf", "server.xml");
    assertTrue(Files.isRegularFile(serverXml), () -> "missing " + serverXml.toAbsolutePath());
    String s = Files.readString(serverXml, StandardCharsets.UTF_8);
    assertTrue(s.contains("<SSLHostConfig"), "source server.xml must nest SSLHostConfig");
    assertTrue(
        s.contains("certificateKeystoreFile=\"${https.keystoreFile}\""),
        "source server.xml Certificate must use ${https.keystoreFile}");
    assertTrue(
        !s.matches("(?s).*(?<![A-Za-z])keystoreFile=\"\\$\\{https\\.keystoreFile\\}\".*"),
        "source server.xml must not put legacy keystoreFile on the Connector");
  }
}
