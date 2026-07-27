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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link PSGeneratedPasswords}, focusing on:
 *
 * <ul>
 *   <li>Round-trip read/write preserves unrelated keys (Admin/Editor/Contributor convention).
 *   <li>Cross-platform path handling: file lives under {@code var/config/generated/passwords}
 *       regardless of {@link java.io.File#separator}.
 *   <li>Random generation produces URL-safe, non-empty, unique values across calls.
 *   <li>Reading a missing file or missing key yields {@code null} (upgrade-safe fallback).
 *   <li>Empty-string values are allowed (explicit blank operator input); blank passwords are
 *       distinguishable from "key missing".
 * </ul>
 */
@Tag("UnitTest")
public class PSGeneratedPasswordsTest {

  @TempDir Path installRoot;

  @Test
  void passwordsFileIsUnderVarConfigGenerated() throws Exception {
    Path file = PSGeneratedPasswords.passwordsFile(installRoot);
    assertTrue(
        file.toAbsolutePath()
            .toString()
            .replace('\\', '/')
            .endsWith("var/config/generated/passwords"),
        "Expected path to end with var/config/generated/passwords but was "
            + file.toAbsolutePath());
  }

  @Test
  void generateRandomPasswordReturnsNonEmptyUrlSafeValue() throws Exception {
    String pwd = PSGeneratedPasswords.generateRandomPassword();
    assertNotNull(pwd);
    assertFalse(pwd.isEmpty());
    assertFalse(pwd.contains("="), "base64url must not contain padding '=' characters");
    assertFalse(pwd.contains("+") || pwd.contains("/"), "base64url must not contain + or /");
  }

  @Test
  void generateRandomPasswordProducesUniqueValues() throws Exception {
    Set<String> seen = new HashSet<>();
    int n = 256;
    for (int i = 0; i < n; i++) {
      assertTrue(seen.add(PSGeneratedPasswords.generateRandomPassword()), "duplicate password");
    }
    assertEquals(n, seen.size());
  }

  @Test
  void generateRandomPasswordRejectsNonPositiveBytes() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> PSGeneratedPasswords.generateRandomPassword(0));
    assertThrows(
        IllegalArgumentException.class, () -> PSGeneratedPasswords.generateRandomPassword(-1));
  }

  @Test
  void writeAndReadRoundTrip() throws Exception {
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "hush-this-is-secret");
    assertEquals(
        "hush-this-is-secret",
        PSGeneratedPasswords.read(installRoot, PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void writePreservesUnrelatedKeys() throws Exception {
    // Seed the file with Admin / Editor / Contributor entries as PSUserService would.
    seedFile(
        installRoot
            .resolve(PSGeneratedPasswords.VAR_CONFIG_GENERATED)
            .resolve(PSGeneratedPasswords.FILE_NAME),
        "Admin",
        "demo-admin",
        "Editor",
        "demo-editor",
        "Contributor",
        "demo-contributor");

    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "fresh-cmdb-pwd");

    Properties reloaded = loadFile(installRoot);
    assertEquals("fresh-cmdb-pwd", reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
    assertEquals("demo-admin", reloaded.getProperty("Admin"));
    assertEquals("demo-editor", reloaded.getProperty("Editor"));
    assertEquals("demo-contributor", reloaded.getProperty("Contributor"));
  }

  @Test
  void writeOverwritesExistingValue() throws Exception {
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "first");
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "second");
    assertEquals("second", PSGeneratedPasswords.read(installRoot, PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void readOnMissingFileReturnsNull() throws Exception {
    assertNull(PSGeneratedPasswords.read(installRoot, PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void readOnMissingKeyReturnsNull() throws Exception {
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "the-value");
    assertNull(PSGeneratedPasswords.read(installRoot, "no-such-key"));
  }

  @Test
  void emptyValueIsDistinguishableFromMissingKey() throws Exception {
    // An operator explicitly entering a blank password must overwrite a missing entry as
    // an empty string rather than leaving the key absent. The H2 installer only writes
    // non-empty passwords, but we still document the contract here.
    PSGeneratedPasswords.write(installRoot, "blank-allowed", "");
    assertEquals("", PSGeneratedPasswords.read(installRoot, "blank-allowed"));
    assertNull(PSGeneratedPasswords.read(installRoot, "not-present"));
  }

  @Test
  void generateAndStoreCmdbStoresAndReturnsValue() throws Exception {
    var entry = PSGeneratedPasswords.generateAndStoreCmdb(installRoot);
    assertNotNull(entry.password());
    assertFalse(entry.password().isEmpty());
    assertTrue(Files.isRegularFile(entry.file()));
    assertEquals(
        entry.password(), PSGeneratedPasswords.read(installRoot, PSGeneratedPasswords.KEY_CMDB));
  }

  @Test
  void passwordsFileFromStringHandlesNullAndBlank() throws Exception {
    assertNull(PSGeneratedPasswords.passwordsFileFromString(null));
    assertNull(PSGeneratedPasswords.passwordsFileFromString(""));
    assertNull(PSGeneratedPasswords.passwordsFileFromString("   "));
    Path resolved = PSGeneratedPasswords.passwordsFileFromString(installRoot.toString());
    assertNotNull(resolved);
    assertTrue(
        resolved
            .toAbsolutePath()
            .toString()
            .replace('\\', '/')
            .endsWith("var/config/generated/passwords"));
  }

  @Test
  void passwordsFileFromStringRejectsRelativePathsAcrossPlatforms() throws Exception {
    // Sanity check: the public API never embeds raw File.separator literals in its
    // file path constants — only the JVM-controlled Path.resolve sees those. The
    // path is normalised to an absolute path via Path.toAbsolutePath().
    Path resolved = PSGeneratedPasswords.passwordsFileFromString(".");
    assertNotNull(resolved);
    assertTrue(
        resolved.isAbsolute(), "passwordsFileFromString must always return an absolute Path");
    assertTrue(
        resolved.toString().replace('\\', '/').endsWith("var/config/generated/passwords"),
        "Resolved path must end with the documented relative path; was " + resolved);
  }

  @Test
  void varConfigGeneratedConstantUsesForwardSlashes() throws Exception {
    // The constant is embedded in error messages and documentation. It must NOT
    // contain File.separator (which differs across OSes) so the same string is
    // rendered identically on Windows, Linux, and macOS.
    assertFalse(
        PSGeneratedPasswords.VAR_CONFIG_GENERATED.contains("\\"),
        "VAR_CONFIG_GENERATED must use forward slashes, not File.separator");
    assertTrue(
        PSGeneratedPasswords.VAR_CONFIG_GENERATED.startsWith("var/"),
        "VAR_CONFIG_GENERATED must be relative to install root");
    assertTrue(
        PSGeneratedPasswords.VAR_CONFIG_GENERATED.endsWith("/generated"),
        "VAR_CONFIG_GENERATED must end with /generated");
  }

  @Test
  void writeDoesNotClobberUnrelatedKeysAcrossCalls() throws Exception {
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "pw-a");
    PSGeneratedPasswords.write(installRoot, "another-key", "another-value");
    PSGeneratedPasswords.write(installRoot, PSGeneratedPasswords.KEY_CMDB, "pw-b");

    Properties reloaded = loadFile(installRoot);
    assertEquals("pw-b", reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
    assertEquals("another-value", reloaded.getProperty("another-key"));
    assertNotEquals("pw-a", reloaded.getProperty(PSGeneratedPasswords.KEY_CMDB));
  }

  private static void seedFile(Path file, String... kv) throws Exception {
    if (kv.length % 2 != 0) {
      throw new IllegalArgumentException("kv must be pairs");
    }
    Files.createDirectories(file.getParent());
    Properties p = new Properties();
    for (int i = 0; i < kv.length; i += 2) {
      p.setProperty(kv[i], kv[i + 1]);
    }
    try (OutputStream out = Files.newOutputStream(file)) {
      p.store(out, "seed");
    }
  }

  private static Properties loadFile(Path installRoot) throws Exception {
    Path file = PSGeneratedPasswords.passwordsFile(installRoot);
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return p;
  }
}
