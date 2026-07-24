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
package com.percussion.install;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Redacts secrets from migration logs and durable reports (FR-017, QC-022).
 *
 * <p>Never log {@code PWD}, password tokens, or JDBC URLs that embed credentials.
 */
public final class PSMigrationSecretsRedactor {

  private static final String REDACTED = "****";

  /** Matches {@code password=...}, {@code pwd=...}, {@code PWD=...} style tokens. */
  private static final Pattern PASSWORD_KEY =
      Pattern.compile(
          "(?i)((?:password|pwd|passwd|secret|token)\\s*[=:]\\s*)([^\\s,;&\"']+)",
          Pattern.CASE_INSENSITIVE);

  /** Matches userinfo in JDBC URLs: {@code jdbc:...://user:pass@host}. */
  private static final Pattern JDBC_USERINFO =
      Pattern.compile("(jdbc:[^:]+://)([^:/@]+):([^@/]+)@", Pattern.CASE_INSENSITIVE);

  private PSMigrationSecretsRedactor() {}

  /**
   * Return a copy of {@code text} with password-like values replaced by {@code ****}.
   *
   * @param text may be null
   * @return redacted string, or null if input was null
   */
  public static String redact(String text) {
    if (text == null) {
      return null;
    }
    String result = PASSWORD_KEY.matcher(text).replaceAll("$1" + REDACTED);
    result = JDBC_USERINFO.matcher(result).replaceAll("$1$2:" + REDACTED + "@");
    // Property-file style PWD=value on its own line or fragment
    if (result.toLowerCase(Locale.ROOT).contains("pwd=")) {
      result =
          result.replaceAll("(?i)(PWD\\s*=\\s*)([^\\s\\r\\n]*)", "$1" + REDACTED);
    }
    return result;
  }

  /**
   * Whether {@code text} appears to still contain an unredacted password-like token.
   *
   * @param text may be null
   * @return true if a non-redacted secret pattern remains
   */
  public static boolean appearsToContainSecret(String text) {
    if (text == null || text.isBlank()) {
      return false;
    }
    String redacted = redact(text);
    return !text.equals(redacted);
  }
}
