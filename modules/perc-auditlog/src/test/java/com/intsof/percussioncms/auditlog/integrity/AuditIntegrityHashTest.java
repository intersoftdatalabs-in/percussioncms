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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.AuditOutcome;
import com.intsof.percussioncms.auditlog.AuditRecord;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link AuditIntegrityHash}. */
class AuditIntegrityHashTest {

  private static final Instant T0 = Instant.parse("2026-08-10T12:00:00Z");
  private static final String ID = "11111111-2222-3333-4444-555555555555";

  @Test
  void sha256Hex_isDeterministicForSameRecord() {
    AuditRecord record = sampleRecord(Map.of("b", "2", "a", "1"));
    String h1 = AuditIntegrityHash.sha256Hex(record);
    String h2 = AuditIntegrityHash.sha256Hex(record);
    assertEquals(64, h1.length());
    assertEquals(h1, h2);
    assertTrue(h1.matches("[0-9a-f]{64}"));
  }

  @Test
  void sha256Hex_attributeKeyOrderDoesNotChangeDigest() {
    AuditRecord insertionOrder = sampleRecord(linked("z", "9", "a", "1", "m", "5"));
    AuditRecord reverseOrder = sampleRecord(linked("m", "5", "a", "1", "z", "9"));
    assertEquals(
        AuditIntegrityHash.sha256Hex(insertionOrder),
        AuditIntegrityHash.sha256Hex(reverseOrder));
  }

  @Test
  void sha256Hex_fieldChangeChangesDigest() {
    AuditRecord base = sampleRecord(Map.of());
    AuditRecord mutated =
        AuditRecord.builder()
            .logId(AuditLogId.of(ID))
            .eventTime(T0)
            .code(AuthenticationErrorCodes.LOGIN_SUCCESS)
            .outcome(AuditOutcome.SUCCESS)
            .actor("other")
            .userMessage("ok")
            .logMessage("ok")
            .formattedLine("[AUTH]-[id] ok")
            .build();
    assertNotEquals(AuditIntegrityHash.sha256Hex(base), AuditIntegrityHash.sha256Hex(mutated));
  }

  @Test
  void sha256Hex_rawFieldsMatchAuditRecord() {
    AuditRecord record = sampleRecord(Map.of("k", "v"));
    String fromRecord = AuditIntegrityHash.sha256Hex(record);
    String fromFields =
        AuditIntegrityHash.sha256Hex(
            record.logId().value(),
            record.eventTime(),
            record.code().module().code(),
            record.code().numericCode(),
            record.code().eventType().name(),
            record.outcome().name(),
            record.actor().orElse(null),
            record.target().orElse(null),
            record.sourceIp().orElse(null),
            record.sourceHost().orElse(null),
            record.sessionIdHash().orElse(null),
            record.userMessage(),
            record.logMessage(),
            record.correlationId().orElse(null),
            AuditIntegrityHash.attributesCanonical(record.attributes()),
            record.serverNode().orElse(null));
    assertEquals(fromRecord, fromFields);
  }

  @Test
  void nullFields_encodeAsEmpty_sameAsEmptyStrings() {
    String withNulls =
        AuditIntegrityHash.canonicalize(
            ID, T0, "AUTH", 1001, null, "SUCCESS", null, null, null, null, null, null, null, null,
            null, null);
    String withEmpty =
        AuditIntegrityHash.canonicalize(
            ID, T0, "AUTH", 1001, "", "SUCCESS", "", "", "", "", "", "", "", "", "", "");
    assertEquals(withEmpty, withNulls);
  }

  @Test
  void matches_acceptsCaseInsensitiveHex() {
    AuditRecord record = sampleRecord(Map.of());
    String hex = AuditIntegrityHash.sha256Hex(record);
    assertTrue(AuditIntegrityHash.matches(record, hex));
    assertTrue(AuditIntegrityHash.matches(record, hex.toUpperCase()));
    assertFalse(AuditIntegrityHash.matches(record, null));
    assertFalse(AuditIntegrityHash.matches(record, "  "));
    assertFalse(AuditIntegrityHash.matches(record, "0".repeat(64)));
  }

  @Test
  void sha256Hex_rejectsNullRecord() {
    assertThrows(NullPointerException.class, () -> AuditIntegrityHash.sha256Hex((AuditRecord) null));
  }

  @Test
  void toLowerHex_knownVector() {
    // SHA-256("") empty string — public test vector
    String empty =
        AuditIntegrityHash.sha256HexOfBytes("".getBytes(StandardCharsets.UTF_8));
    assertEquals(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", empty);
  }

  @Test
  void attributesCanonical_sortsAndSkipsNullKeys() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("b", "2");
    map.put(null, "x");
    map.put("a", null);
    String expected = "a=" + AuditIntegrityHash.ATTR_SEP + "b=2";
    assertEquals(expected, AuditIntegrityHash.attributesCanonical(map));
  }

  private static AuditRecord sampleRecord(Map<String, String> attrs) {
    return AuditRecord.builder()
        .logId(AuditLogId.of(ID))
        .eventTime(T0)
        .code(AuthenticationErrorCodes.LOGIN_SUCCESS)
        .outcome(AuditOutcome.SUCCESS)
        .actor("jdoe")
        .target("session")
        .sourceIp("10.0.0.1")
        .sourceHost("cms1")
        .sessionIdHash("abc")
        .correlationId("corr-1")
        .userMessage("login ok")
        .logMessage("login ok detail")
        .formattedLine("[AUTH-1001]-[" + ID + "] login ok")
        .attributes(attrs)
        .serverNode("node-a")
        .build();
  }

  private static Map<String, String> linked(String... kv) {
    LinkedHashMap<String, String> m = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put(kv[i], kv[i + 1]);
    }
    return m;
  }
}
