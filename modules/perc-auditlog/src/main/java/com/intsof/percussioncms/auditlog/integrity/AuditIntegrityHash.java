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
package com.intsof.percussioncms.auditlog.integrity;

import com.intsof.percussioncms.auditlog.AuditRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Deterministic SHA-256 integrity digest over canonical audit-row field material.
 *
 * <p>Intended for export/query verification helpers and operator tools. This is <strong>not</strong>
 * a digital signature, HMAC, or WORM guarantee: a privileged actor who can rewrite both the row and
 * the digest can still forge a match. See {@code
 * docs/ai-generated/tasks/system-audit-log/ops-federal-runbook.md} §7.
 *
 * <p>Canonical form (UTF-8): fields joined with unit separator {@link #FIELD_SEP}, null → empty,
 * attributes keys sorted and joined with {@link #ATTR_SEP} as {@code key=value}.
 */
public final class AuditIntegrityHash {

  /** Digest algorithm (JDK standard name). */
  public static final String ALGORITHM = "SHA-256";

  /** Unicode unit separator between top-level fields. */
  public static final char FIELD_SEP = '\u001f';

  /** Unicode record separator between sorted attribute pairs. */
  public static final char ATTR_SEP = '\u001e';

  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private AuditIntegrityHash() {}

  /**
   * SHA-256 lowercase hex over the canonical encoding of an {@link AuditRecord}.
   *
   * @param record non-null audit record (already redacted messages as stored)
   * @return 64-char lowercase hex digest
   */
  public static String sha256Hex(AuditRecord record) {
    Objects.requireNonNull(record, "record");
    String eventType =
        record.code().eventType() == null ? "" : record.code().eventType().name();
    return sha256Hex(
        record.logId().value(),
        record.eventTime(),
        record.code().module().code(),
        record.code().numericCode(),
        eventType,
        record.outcome().name(),
        record.actor().orElse(null),
        record.target().orElse(null),
        record.sourceIp().orElse(null),
        record.sourceHost().orElse(null),
        record.sessionIdHash().orElse(null),
        record.userMessage(),
        record.logMessage(),
        record.correlationId().orElse(null),
        attributesCanonical(record.attributes()),
        record.serverNode().orElse(null));
  }

  /**
   * SHA-256 lowercase hex over raw field material (e.g. REST DTO / export mapping).
   *
   * @param auditId audit UUID string
   * @param eventTime event instant; {@code null} treated as empty time string
   * @param moduleCode module code (e.g. {@code AUTH})
   * @param messageCode numeric message code
   * @param eventType event type name or null/empty
   * @param outcome outcome name
   * @param actor actor
   * @param target target
   * @param sourceIp source IP
   * @param sourceHost source host
   * @param sessionIdHash hashed session id
   * @param userMessage user-facing message
   * @param logMessage log-facing message
   * @param correlationId correlation id
   * @param attributesCanonical already-canonical attribute string (use {@link
   *     #attributesCanonical(Map)} when starting from a map)
   * @param serverNode server node
   * @return 64-char lowercase hex digest
   */
  public static String sha256Hex(
      String auditId,
      Instant eventTime,
      String moduleCode,
      int messageCode,
      String eventType,
      String outcome,
      String actor,
      String target,
      String sourceIp,
      String sourceHost,
      String sessionIdHash,
      String userMessage,
      String logMessage,
      String correlationId,
      String attributesCanonical,
      String serverNode) {
    String canonical =
        canonicalize(
            auditId,
            eventTime,
            moduleCode,
            messageCode,
            eventType,
            outcome,
            actor,
            target,
            sourceIp,
            sourceHost,
            sessionIdHash,
            userMessage,
            logMessage,
            correlationId,
            attributesCanonical,
            serverNode);
    return sha256HexOfBytes(canonical.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Builds the stable attribute encoding: keys sorted lexicographically (Unicode code-point order
   * via {@link TreeMap}), each pair {@code key=value}, pairs joined with {@link #ATTR_SEP}.
   *
   * @param attributes may be null or empty → empty string
   * @return canonical attribute material
   */
  public static String attributesCanonical(Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return "";
    }
    TreeMap<String, String> sorted = new TreeMap<>();
    for (Map.Entry<String, String> e : attributes.entrySet()) {
      if (e.getKey() == null) {
        continue;
      }
      sorted.put(e.getKey(), e.getValue() == null ? "" : e.getValue());
    }
    if (sorted.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> e : sorted.entrySet()) {
      if (!first) {
        sb.append(ATTR_SEP);
      }
      first = false;
      sb.append(e.getKey()).append('=').append(e.getValue());
    }
    return sb.toString();
  }

  /**
   * @param record non-null record
   * @param expectedHex expected digest (case-insensitive); null/blank → false
   * @return true when digests match
   */
  public static boolean matches(AuditRecord record, String expectedHex) {
    if (expectedHex == null || expectedHex.isBlank()) {
      return false;
    }
    String actual = sha256Hex(record);
    return actual.equalsIgnoreCase(expectedHex.trim());
  }

  /**
   * Canonical UTF-8 payload string (field order fixed). Exposed for tests and external verifiers
   * that reimplement the same layout.
   *
   * @param auditId audit UUID string (null → empty)
   * @param eventTime event instant ({@code null} → empty time string)
   * @param moduleCode module code (null → empty)
   * @param messageCode numeric message code
   * @param eventType event type name (null → empty)
   * @param outcome outcome name (null → empty)
   * @param actor actor (null → empty)
   * @param target target (null → empty)
   * @param sourceIp source IP (null → empty)
   * @param sourceHost source host (null → empty)
   * @param sessionIdHash hashed session id (null → empty)
   * @param userMessage user-facing message (null → empty)
   * @param logMessage log-facing message (null → empty)
   * @param correlationId correlation id (null → empty)
   * @param attributesCanonical pre-canonical attributes (null → empty)
   * @param serverNode server node (null → empty)
   * @return canonical field string joined with {@link #FIELD_SEP}
   */
  public static String canonicalize(
      String auditId,
      Instant eventTime,
      String moduleCode,
      int messageCode,
      String eventType,
      String outcome,
      String actor,
      String target,
      String sourceIp,
      String sourceHost,
      String sessionIdHash,
      String userMessage,
      String logMessage,
      String correlationId,
      String attributesCanonical,
      String serverNode) {
    StringBuilder sb = new StringBuilder(256);
    append(sb, auditId);
    sb.append(FIELD_SEP);
    append(sb, eventTime == null ? null : eventTime.toString());
    sb.append(FIELD_SEP);
    append(sb, moduleCode);
    sb.append(FIELD_SEP);
    sb.append(Integer.toString(messageCode));
    sb.append(FIELD_SEP);
    append(sb, eventType);
    sb.append(FIELD_SEP);
    append(sb, outcome);
    sb.append(FIELD_SEP);
    append(sb, actor);
    sb.append(FIELD_SEP);
    append(sb, target);
    sb.append(FIELD_SEP);
    append(sb, sourceIp);
    sb.append(FIELD_SEP);
    append(sb, sourceHost);
    sb.append(FIELD_SEP);
    append(sb, sessionIdHash);
    sb.append(FIELD_SEP);
    append(sb, userMessage);
    sb.append(FIELD_SEP);
    append(sb, logMessage);
    sb.append(FIELD_SEP);
    append(sb, correlationId);
    sb.append(FIELD_SEP);
    append(sb, attributesCanonical);
    sb.append(FIELD_SEP);
    append(sb, serverNode);
    return sb.toString();
  }

  private static void append(StringBuilder sb, String value) {
    if (value != null) {
      sb.append(value);
    }
  }

  static String sha256HexOfBytes(byte[] input) {
    Objects.requireNonNull(input, "input");
    MessageDigest md;
    try {
      md = MessageDigest.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is required on every supported JDK; surface as unchecked if the JRE is broken.
      throw new IllegalStateException(ALGORITHM + " not available", e);
    }
    byte[] digest = md.digest(input);
    return toLowerHex(digest);
  }

  static String toLowerHex(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xff;
      out[i * 2] = HEX[v >>> 4];
      out[i * 2 + 1] = HEX[v & 0x0f];
    }
    return new String(out);
  }

  /**
   * Normalizes a hex string for comparison (trim + lower-case).
   *
   * @param hex raw hex or {@code null}
   * @return trimmed lowercase hex, or empty string when {@code hex} is null
   */
  public static String normalizeHex(String hex) {
    if (hex == null) {
      return "";
    }
    return hex.trim().toLowerCase(Locale.ROOT);
  }
}

