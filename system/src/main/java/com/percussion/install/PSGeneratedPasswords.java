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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * Persistence helper for the {@code var/config/generated/passwords} file used by Percussion CMS to
 * share installer- and runtime- <strong>system-generated</strong> credentials with operators and
 * tooling.
 *
 * <p>The file is a Java {@link Properties} store: {@code key=value} lines, one credential per key.
 * Existing entries are preserved across writes so unrelated credentials (e.g. Admin, Editor,
 * Contributor populated by {@code PSUserService}) are not disturbed when this helper updates an
 * entry.
 *
 * <p><strong>Scope:</strong> this file is reserved for credentials the system auto-generates and
 * the operator never types. Operator-supplied secrets — including CMS DB passwords typed during
 * interactive installs — live only in {@code rxconfig/Installer/rxrepository.properties} and the
 * Jetty {@code perc-ds.properties} (encrypted), not here. Concrete entries owned by the system:
 *
 * <ul>
 *   <li>{@code cmdb}: cryptographically random CMS repository password for the embedded H2
 *       backend (silent / unattended installs only). Random generation avoids the "Wrong user
 *       name or password" class of failures when the JDBC driver and the on-disk
 *       {@code Repository/CMDB.mv.db} disagree about credentials (issue #548 / #1500).
 *   <li>{@code Admin}, {@code Editor}, {@code Contributor} (and other demo users): random
 *       passwords for the built-in accounts managed by {@code PSUserService}.
 * </ul>
 *
 * <p>Reads tolerate a missing or partially written file: callers get {@code null} / empty values
 * so upgrade paths that pre-date this helper continue to read the password they already have on
 * disk in {@code rxrepository.properties}.
 *
 * <p><strong>Security:</strong> the generated passwords file is installer-managed; treat its
 * contents as confidential and never log it.
 */
public final class PSGeneratedPasswords {

  /**
   * Property key for the embedded CMS repository database password (H2 default backend, Derby
   * legacy). Read by the runtime when the value differs from the empty default that ships in {@code
   * rxrepository.properties}.
   */
  public static final String KEY_CMDB = "cmdb";

  /** Default file name (no directory component) under {@code var/config/generated/}. */
  public static final String FILE_NAME = "passwords";

  /**
   * Directory under the install root that holds installer-generated secrets. Always rendered with
   * forward slashes in operator-facing paths; {@link Path} APIs handle the actual separator.
   */
  public static final String VAR_CONFIG_GENERATED = "var" + "/config" + "/generated";

  /** Default password length (bytes of entropy before base64url encoding). */
  public static final int DEFAULT_PASSWORD_BYTES = 24;

  private static final char[] BASE64URL =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

  private PSGeneratedPasswords() {}

  /**
   * Generates a URL-safe, cryptographically random password.
   *
   * @param bytes entropy length before encoding; must be positive. Default is {@link
   *     #DEFAULT_PASSWORD_BYTES}.
   * @return base64url-encoded password, never {@code null} or empty.
   */
  public static String generateRandomPassword(int bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("bytes must be positive: " + bytes);
    }
    byte[] raw = new byte[bytes];
    new SecureRandom().nextBytes(raw);
    StringBuilder sb = new StringBuilder((bytes * 4 + 2) / 3);
    int i = 0;
    for (; i + 2 < raw.length; i += 3) {
      int n = ((raw[i] & 0xff) << 16) | ((raw[i + 1] & 0xff) << 8) | (raw[i + 2] & 0xff);
      sb.append(BASE64URL[(n >> 18) & 0x3f]);
      sb.append(BASE64URL[(n >> 12) & 0x3f]);
      sb.append(BASE64URL[(n >> 6) & 0x3f]);
      sb.append(BASE64URL[n & 0x3f]);
    }
    int remaining = raw.length - i;
    if (remaining == 1) {
      int n = (raw[i] & 0xff) << 16;
      sb.append(BASE64URL[(n >> 18) & 0x3f]);
      sb.append(BASE64URL[(n >> 12) & 0x3f]);
    } else if (remaining == 2) {
      int n = ((raw[i] & 0xff) << 16) | ((raw[i + 1] & 0xff) << 8);
      sb.append(BASE64URL[(n >> 18) & 0x3f]);
      sb.append(BASE64URL[(n >> 12) & 0x3f]);
      sb.append(BASE64URL[(n >> 6) & 0x3f]);
    }
    return sb.toString();
  }

  /** Convenience: {@link #generateRandomPassword(int)} with {@link #DEFAULT_PASSWORD_BYTES}. */
  public static String generateRandomPassword() {
    return generateRandomPassword(DEFAULT_PASSWORD_BYTES);
  }

  /**
   * Resolves the {@code passwords} file under the supplied install root.
   *
   * @param installRoot CMS install root directory; must not be {@code null}.
   * @return absolute path to {@code var/config/generated/passwords}, never {@code null}.
   */
  public static Path passwordsFile(Path installRoot) {
    if (installRoot == null) {
      throw new IllegalArgumentException("installRoot must not be null");
    }
    return installRoot
        .toAbsolutePath()
        .normalize()
        .resolve(VAR_CONFIG_GENERATED)
        .resolve(FILE_NAME);
  }

  /**
   * Reads a single entry from the generated passwords file, preserving all other entries.
   *
   * @param installRoot CMS install root directory; must not be {@code null}.
   * @param key property key to look up; must not be {@code null}.
   * @return trimmed value, or {@code null} when the file or key is missing.
   * @throws IOException when the file exists but cannot be read.
   */
  public static String read(Path installRoot, String key) throws IOException {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    Path file = passwordsFile(installRoot);
    if (!Files.isRegularFile(file)) {
      return null;
    }
    Properties props = load(file);
    String value = props.getProperty(key);
    return value == null ? null : value.trim();
  }

  /**
   * Writes a single entry to the generated passwords file. Existing entries (e.g. Admin, Editor,
   * Contributor) are preserved. The file is created if missing; its parent directory is created if
   * needed. Writes are atomic via a temporary file in the same directory.
   *
   * @param installRoot CMS install root directory; must not be {@code null}.
   * @param key property key to set; must not be {@code null}.
   * @param value password value to persist; must not be {@code null} (use empty string when an
   *     operator explicitly supplied a blank password).
   * @return absolute path of the written file.
   * @throws IOException when the file cannot be written.
   */
  public static Path write(Path installRoot, String key, String value) throws IOException {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    if (value == null) {
      throw new IllegalArgumentException("value must not be null (use empty string for blank)");
    }
    Path file = passwordsFile(installRoot);
    Path parent = file.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    Properties props = new Properties();
    if (Files.isRegularFile(file)) {
      props = load(file);
    }
    props.setProperty(key, value);

    Path temp = Files.createTempFile(parent, FILE_NAME, ".tmp");
    try {
      try (OutputStream out = Files.newOutputStream(temp);
          Writer writer = new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8)) {
        props.store(
            writer,
            "Installer-generated credentials. Do not edit; re-run installer to regenerate.");
      }
      Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temp);
    }
    return file;
  }

  private static Properties load(Path file) throws IOException {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file);
        Reader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    return props;
  }

/**
   * Convenience: write a freshly generated random password under {@link #KEY_CMDB} and return
   * both the file path and the generated value.
   *
   * @param installRoot CMS install root directory; must not be {@code null}.
   * @return a record with the generated password and the absolute file path it was written to.
   * @throws IOException when the file cannot be written.
   */
  public static GeneratedEntry generateAndStoreCmdb(Path installRoot) throws IOException {
    String password = generateRandomPassword();
    Path file = write(installRoot, KEY_CMDB, password);
    return new GeneratedEntry(password, file);
  }

  /**
   * Record describing a single generated-password write.
   *
   * @param password the credential value that was stored
   * @param file absolute path of the {@code var/config/generated/passwords} file
   */
  public record GeneratedEntry(String password, Path file) {}

  /**
   * Convenience for callers that hold an install root as a {@link String} (e.g. ANT build files,
   * legacy installer entry points).
   *
   * @param installRoot CMS install root directory as a string; may be {@code null}.
   * @return absolute path to {@code var/config/generated/passwords}; {@code null} when {@code
   *     installRoot} is {@code null} or blank.
   */
  public static Path passwordsFileFromString(String installRoot) {
    if (installRoot == null || installRoot.trim().isEmpty()) {
      return null;
    }
    return passwordsFile(Paths.get(installRoot.trim()));
  }
}
