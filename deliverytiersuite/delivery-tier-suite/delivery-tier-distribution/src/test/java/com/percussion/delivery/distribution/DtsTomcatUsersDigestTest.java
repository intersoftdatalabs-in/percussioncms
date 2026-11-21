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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for #6939: the shipped {@code Deployment/Server/conf/tomcat-users.xml} must
 * carry the SHA-1 digest of the documented default deliverymanager secret, not the secret itself.
 *
 * <p>Cargo writes the pom {@code <users>} block verbatim into {@code conf/tomcat-users.xml}, but
 * the shipped {@code server.xml} authenticates the {@code UserDatabaseRealm} through {@code
 * MessageDigestCredentialHandler algorithm="SHA"}, which compares SHA-1 hex digests. A verbatim
 * secret in the stored slot can never match what the handler computes from the presented
 * credential, so every BASIC-authenticated retrieval call (form summaries, CSV export) answers 401
 * until the credential is reseeded.
 *
 * <p>The jetty9x profile keeps its own Jetty {@code HashLoginService} realm ({@code
 * dts-realm.properties}) and is intentionally out of scope: only the default tomcat11x build is
 * asserted here.
 *
 * <p>The shipping-jar assertion is a no-op when {@code target/delivery-tier-distribution.jar} is
 * absent (IDE / ad-hoc runs); the source-tree assertions always run.
 */
class DtsTomcatUsersDigestTest {

  // mkd-code-review[secrets.heuristic]: adjudicated intentional (issue #6939) — published
  // default/test credential already stored verbatim in pom.xml cargo <users> and QA matrices;
  // not a live secret, and the test must name it to assert the stored-digest coupling.
  /** Secret operators present over BASIC auth (also the QA matrix client fallback). */
  private static final String DOCUMENTED_DEFAULT_SECRET = "f2c57870308dc87f432e5912d4de6f8e322721ba";

  // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #6939, same as
  // DtsInstallerJarContainsTomcatTreeTest) — java.util.zip entry names always use '/',
  // never an OS filesystem separator.
  private static final String SHIPPED_TOMCAT_USERS_ENTRY =
      "distribution/Deployment/Server/conf/tomcat-users.xml";

  private static String sha1Hex(String secret) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-1").digest(secret.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException impossible) {
      throw new AssertionError("JDK must provide SHA-1", impossible);
    }
  }

  /**
   * Source-tree guard: the default (tomcat11x) cargo {@code <users>} password must be the SHA-1
   * digest of the documented secret, and the shipped {@code server.xml} must still declare the SHA
   * credential handler — the two sides of the #6939 coupling. Changing either file without the
   * other fails here by design.
   */
  @Test
  void sourceCargoUsersStoresDigestOfDocumentedSecret() throws IOException {
    Path pom = Path.of("pom.xml");
    assertTrue(Files.isRegularFile(pom), () -> "missing " + pom.toAbsolutePath());
    String pomXml = Files.readString(pom, StandardCharsets.UTF_8);

    // Only the default build is in scope; the jetty9x profile below keeps its own realm.
    int profilesAt = pomXml.indexOf("<profiles>");
    assertTrue(profilesAt > 0, "pom.xml must keep the <profiles> section boundary this guard splits on");
    String defaultBuild = pomXml.substring(0, profilesAt);

    String digest = sha1Hex(DOCUMENTED_DEFAULT_SECRET);
    assertTrue(
        defaultBuild.contains("<password>" + digest + "</password>"),
        "default-build cargo <users> password must be sha1(documented default secret) = "
            + digest
            + " or the shipped tomcat-users.xml cannot authenticate (#6939)");
    assertTrue(
        !defaultBuild.contains("<password>" + DOCUMENTED_DEFAULT_SECRET + "</password>"),
        "default-build cargo <users> must not store the documented secret verbatim;"
            + " the SHA credential handler would never match it (#6939)");

    Path serverXml = Path.of("src", "main", "tomcat11", "conf", "server.xml");
    assertTrue(Files.isRegularFile(serverXml), () -> "missing " + serverXml.toAbsolutePath());
    String server = Files.readString(serverXml, StandardCharsets.UTF_8);
    assertTrue(
        server.contains("MessageDigestCredentialHandler")
            && server.contains("algorithm=\"SHA\""),
        "shipped server.xml must keep MessageDigestCredentialHandler algorithm=\"SHA\";"
            + " removing it changes what the stored tomcat-users.xml digest must be (#6939)");
  }

  /** Shipping-jar guard: the assembled tomcat-users.xml ships the digest, not the secret. */
  @Test
  void shippingJarTomcatUsersCarriesDigestNotSecret() throws IOException {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    String digest = sha1Hex(DOCUMENTED_DEFAULT_SECRET);
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      ZipEntry entry = zip.getEntry(SHIPPED_TOMCAT_USERS_ENTRY);
      if (entry == null) {
        fail("Missing zip entry: " + SHIPPED_TOMCAT_USERS_ENTRY + ";"
            + " Cargo package was not staged before jar packaging."
            + " See antrun id=stage-cargo-package-into-assembly.");
      }
      String usersXml;
      try (InputStream in = zip.getInputStream(entry)) {
        usersXml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertTrue(
          usersXml.contains("name=\"ps_manager\""),
          "shipped tomcat-users.xml must define the ps_manager deliverymanager user");
      assertTrue(
          usersXml.contains("password=\"" + digest + "\""),
          "shipped tomcat-users.xml must store sha1(documented default secret) for ps_manager"
              + " or fresh installs answer 401 (#6939)");
      assertTrue(
          !usersXml.contains("password=\"" + DOCUMENTED_DEFAULT_SECRET + "\""),
          "shipped tomcat-users.xml must not store the documented secret verbatim (#6939)");
    }
  }
}
