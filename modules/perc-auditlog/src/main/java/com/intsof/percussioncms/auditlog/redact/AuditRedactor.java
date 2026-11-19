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
package com.intsof.percussioncms.auditlog.redact;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Redacts sensitive material from user and log messages (and attribute maps) before dual-write.
 *
 * <p>Applied to <em>both</em> userMessage and logMessage channels.
 *
 * <p>Regex usage is intentionally bounded and linear-time where practical (length cap before
 * matching; JWT redaction uses a linear scan instead of a backtracking {@code Pattern} — CodeQL
 * {@code java/polynomial-redos} / alert #1948).
 */
public final class AuditRedactor {

  public static final String REDACTED = "[REDACTED]";

  /** Max characters processed for redaction; longer inputs are truncated first (ReDoS bound). */
  private static final int MAX_FIELD_LENGTH = 4000;

  private static final Pattern PASSWORD_KV =
      Pattern.compile(
          // mkd-code-review[secrets.heuristic]: adjudicated intentional (issue #50, same as M2) — regex scrubs credentials from audit logs, not a hardcoded secret.
          "(?i)(password|passwd|pwd|secret|token|api[_-]?key|authorization|bearer)\\s*([:=])\\s*([^\\s,;&]+)");

  private static final Pattern BASIC_AUTH =
      Pattern.compile("(?i)(basic)\\s+[A-Za-z0-9+/=]{8,}");

  private static final Pattern URL_CREDENTIALS =
      Pattern.compile("(?i)(://)([^/@\\s:]+):([^/@\\s]+)@");

  private static final Pattern CONNECTION_PASSWORD =
      Pattern.compile(
          // mkd-code-review[secrets.heuristic]: adjudicated intentional (issue #50, same as M2) — regex scrubs connection credentials from audit logs, not a hardcoded secret.
          "(?i)(password|pwd)=([^;&\\s]+)");

  private static final Pattern OTPAUTH_URI = Pattern.compile("(?i)otpauth://[^\\s\"'<>]+");

  private static final Pattern MFA_SECRET_KV =
      Pattern.compile(
          "(?i)(recovery[_-]?code|totp[_-]?secret|manual[_-]?secret|otp[_-]?secret|backup[_-]?code|setup[_-]?key|totp|otp|challenge)\\s*([:=])\\s*([^\\s,;&]+)");

  private static final Pattern SENSITIVE_QUERY_PARAM =
      Pattern.compile("(?i)([?&](?:token|secret|code|otp|totp|recovery_code)=)([^&\\s\"'<>]+)");

  private static final int MIN_BASE32_SECRET_LEN = 26;

  /**
   * Redact a single string value. {@code null} becomes empty string after redaction pipeline.
   */
  public String redact(String input) {
    if (input == null || input.isEmpty()) {
      return input == null ? "" : input;
    }
    // Bound input length before any regex so match cost cannot grow unbounded (ReDoS).
    String s =
        input.length() > MAX_FIELD_LENGTH
            ? input.substring(0, MAX_FIELD_LENGTH) + "…"
            : input;
    s = OTPAUTH_URI.matcher(s).replaceAll("otpauth://" + REDACTED);
    s = SENSITIVE_QUERY_PARAM.matcher(s).replaceAll("$1" + REDACTED);
    s = MFA_SECRET_KV.matcher(s).replaceAll("$1$2" + REDACTED);
    s = PASSWORD_KV.matcher(s).replaceAll("$1$2" + REDACTED);
    s = CONNECTION_PASSWORD.matcher(s).replaceAll("$1=" + REDACTED);
    s = URL_CREDENTIALS.matcher(s).replaceAll("$1" + REDACTED + ":" + REDACTED + "@");
    s = redactJwtLikeTokens(s);
    s = BASIC_AUTH.matcher(s).replaceAll("$1 " + REDACTED);
    s = redactRecoveryCodeTokens(s);
    s = redactBase32Secrets(s);
    return s;
  }

  /** Redacts standalone Crockford Base32 recovery codes formatted {@code XXXXX-XXXXX}. */
  static String redactRecoveryCodeTokens(String s) {
    if (s == null || s.length() < 11) {
      return s;
    }
    final int n = s.length();
    StringBuilder out = null;
    int copyFrom = 0;
    int i = 0;
    while (i <= n - 11) {
      if (isRecoveryCodeAt(s, i)) {
        if (out == null) {
          out = new StringBuilder(n);
        }
        out.append(s, copyFrom, i).append(REDACTED);
        i += 11;
        copyFrom = i;
        continue;
      }
      i++;
    }
    return out == null ? s : out.append(s, copyFrom, n).toString();
  }

  private static boolean isRecoveryCodeAt(String s, int start) {
    if (start > 0 && !isTokenBoundaryBefore(s.charAt(start - 1))) {
      return false;
    }
    int end = start + 11;
    if (end < s.length() && !isTokenBoundaryBefore(s.charAt(end))) {
      return false;
    }
    if (s.charAt(start + 5) != '-') {
      return false;
    }
    return hasValidRecoveryBody(s, start);
  }

  private static boolean hasValidRecoveryBody(String s, int start) {
    boolean sawDigit = false;
    for (int j = 0; j < 11; j++) {
      if (j == 5) {
        continue;
      }
      char c = s.charAt(start + j);
      if (!isUpperAlnum(c)) {
        return false;
      }
      if (c >= '0' && c <= '9') {
        sawDigit = true;
      }
    }
    return sawDigit;
  }

  /** Redacts standalone Base32 TOTP setup secrets ({@code [A-Z2-7]{26,}}). */
  static String redactBase32Secrets(String s) {
    if (s == null || s.length() < MIN_BASE32_SECRET_LEN) {
      return s;
    }
    final int n = s.length();
    StringBuilder out = null;
    int copyFrom = 0;
    int i = 0;
    while (i < n) {
      int runEnd = findBase32SecretEnd(s, i);
      if (runEnd < 0) {
        i = advancePastAlnum(s, i);
        continue;
      }
      if (out == null) {
        out = new StringBuilder(n);
      }
      out.append(s, copyFrom, i).append(REDACTED);
      i = runEnd;
      copyFrom = runEnd;
    }
    return out == null ? s : out.append(s, copyFrom, n).toString();
  }

  private static int findBase32SecretEnd(String s, int start) {
    if (start > 0 && isAsciiAlnum(s.charAt(start - 1))) {
      return -1;
    }
    int i = start;
    while (i < s.length() && isUpperBase32Char(s.charAt(i))) {
      i++;
    }
    int len = i - start;
    if (len < MIN_BASE32_SECRET_LEN) {
      return -1;
    }
    if (i < s.length() && isAsciiAlnum(s.charAt(i))) {
      return -1;
    }
    return i;
  }

  private static int advancePastAlnum(String s, int pos) {
    int i = pos;
    if (i < s.length() && isAsciiAlnum(s.charAt(i))) {
      while (i < s.length() && isAsciiAlnum(s.charAt(i))) {
        i++;
      }
      return i;
    }
    return i + 1;
  }

  private static boolean isTokenBoundaryBefore(char c) {
    return !isAsciiAlnum(c) && c != '-';
  }

  private static boolean isUpperBase32Char(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= '2' && c <= '7');
  }

  private static boolean isUpperAlnum(char c) {
    return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }

  private static boolean isAsciiAlnum(char c) {
    return isUpperAlnum(c) || (c >= 'a' && c <= 'z');
  }

  /**
   * Linear-time redaction of JWT-shaped tokens ({@code eyJ...eyJ...sig}) without a backtracking
   * regex. Replaces the former {@code Pattern} that CodeQL flagged as polynomial ReDoS.
   */
  static String redactJwtLikeTokens(String s) {
    if (s == null || s.length() < 10) {
      return s;
    }
    final int n = s.length();
    StringBuilder out = null;
    int copyFrom = 0;
    int i = 0;
    while (i < n) {
      int start = indexOfJwtPrefix(s, i);
      if (start < 0) {
        break;
      }
      int end = matchJwtFrom(s, start);
      if (end < 0) {
        i = start + 1;
        continue;
      }
      if (out == null) {
        out = new StringBuilder(n);
      }
      out.append(s, copyFrom, start);
      out.append(REDACTED);
      copyFrom = end;
      i = end;
    }
    if (out == null) {
      return s;
    }
    out.append(s, copyFrom, n);
    return out.toString();
  }

  /**
   * Find next index of {@code eyJ} that can start a JWT (word-ish boundary before it).
   *
   * @return start index or -1
   */
  private static int indexOfJwtPrefix(String s, int from) {
    final int n = s.length();
    int i = from;
    while (i <= n - 3) {
      int at = s.indexOf("eyJ", i);
      if (at < 0) {
        return -1;
      }
      if (at == 0 || !isJwtBodyChar(s.charAt(at - 1))) {
        return at;
      }
      i = at + 1;
    }
    return -1;
  }

  /**
   * If {@code start} begins a three-segment base64url JWT ({@code header.payload.sig}), return the
   * exclusive end index; otherwise -1.
   */
  private static int matchJwtFrom(String s, int start) {
    // header: eyJ + base64url body
    int i = start;
    if (i + 3 > s.length() || !s.startsWith("eyJ", i)) {
      return -1;
    }
    i = consumeJwtBody(s, i + 3);
    if (i < 0 || i >= s.length() || s.charAt(i) != '.') {
      return -1;
    }
    i++; // skip '.'
    // payload must also look like base64 JSON (eyJ…)
    if (i + 3 > s.length() || !s.startsWith("eyJ", i)) {
      return -1;
    }
    i = consumeJwtBody(s, i + 3);
    if (i < 0 || i >= s.length() || s.charAt(i) != '.') {
      return -1;
    }
    i++; // skip '.'
    // signature: at least one base64url char
    int sigStart = i;
    i = consumeJwtBody(s, i);
    if (i < 0 || i == sigStart) {
      return -1;
    }
    // trailing boundary: end or non-body char
    if (i < s.length() && isJwtBodyChar(s.charAt(i))) {
      return -1;
    }
    return i;
  }

  /** Consume maximal {@code [A-Za-z0-9_-]} run starting at {@code from}; return end index. */
  private static int consumeJwtBody(String s, int from) {
    int i = from;
    final int n = s.length();
    while (i < n && isJwtBodyChar(s.charAt(i))) {
      i++;
    }
    return i;
  }

  private static boolean isJwtBodyChar(char c) {
    return (c >= 'A' && c <= 'Z')
        || (c >= 'a' && c <= 'z')
        || (c >= '0' && c <= '9')
        || c == '_'
        || c == '-';
  }

  /** Redact each value in a map; keys are preserved as-is. */
  public Map<String, String> redactAttributes(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return Map.of();
    }
    Map<String, String> out = new LinkedHashMap<>();
    for (Map.Entry<String, String> e : attributes.entrySet()) {
      String key = Objects.requireNonNull(e.getKey(), "attribute key");
      if (isSensitiveKey(key)) {
        out.put(key, REDACTED);
      } else {
        out.put(key, redact(e.getValue()));
      }
    }
    return out;
  }

  /** Redact params used for template substitution (passwords never enter messages). */
  public Object[] redactParams(Object... params) {
    if (params == null || params.length == 0) {
      return params == null ? new Object[0] : params;
    }
    Object[] out = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      Object p = params[i];
      if (p == null) {
        out[i] = null;
      } else if (p instanceof String s) {
        out[i] = redact(s);
      } else {
        out[i] = redact(String.valueOf(p));
      }
    }
    return out;
  }

  private static boolean isSensitiveKey(String key) {
    String k = key.toLowerCase();
    return k.contains("password")
        || k.contains("passwd")
        || k.contains("secret")
        || k.contains("token")
        || k.contains("apikey")
        || k.contains("api_key")
        || k.contains("authorization")
        || k.contains("credential")
        || k.contains("otp")
        || k.contains("totp")
        || k.contains("recovery")
        || k.contains("backup")
        || k.contains("challenge")
        || k.contains("passkey");
  }
}
