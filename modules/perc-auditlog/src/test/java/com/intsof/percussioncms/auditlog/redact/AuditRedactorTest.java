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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditRedactorTest {

  private final AuditRedactor redactor = new AuditRedactor();

  @Test
  void redactsPasswordKeyValue() {
    String out = redactor.redact("password=secret123 user=jdoe");
    assertTrue(out.contains(AuditRedactor.REDACTED));
    assertFalse(out.contains("secret123"));
  }

  @Test
  void redactsUrlCredentials() {
    String out = redactor.redact("jdbc:mysql://user:pass@host/db");
    assertFalse(out.contains("user:pass@"));
    assertTrue(out.contains(AuditRedactor.REDACTED));
  }

  @Test
  void redactsSensitiveAttributeKeys() {
    Map<String, String> out =
        redactor.redactAttributes(Map.of("password", "x", "username", "jdoe"));
    assertEquals(AuditRedactor.REDACTED, out.get("password"));
    assertEquals("jdoe", out.get("username"));
  }

  @Test
  void redactsJwtLikeToken() {
    String jwt =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
    String out = redactor.redact("token=" + jwt);
    assertFalse(out.contains("eyJhbGciOiJIUzI1NiJ9"));
  }
}
