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
package com.intsof.percussioncms.auditlog.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.codes.AuthenticationErrorCodes;
import org.junit.jupiter.api.Test;

class MessageTemplateFormatterTest {

  @Test
  void formatReplacesSequentialPlaceholders() {
    assertEquals(
        "User jdoe logged in successfully",
        MessageTemplateFormatter.format("User {} logged in successfully", "jdoe"));
  }

  @Test
  void formatLeavesPlaceholderWhenParamMissing() {
    assertEquals("a {} c", MessageTemplateFormatter.format("a {} c"));
  }

  @Test
  void formatLineBuildsCanonicalForm() {
    AuditLogId id = AuditLogId.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    String line =
        MessageTemplateFormatter.formatLine(
            AuthenticationErrorCodes.LOGIN_SUCCESS, id, "User jdoe logged in successfully");
    assertEquals(
        "[AUTH-1001]-[aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee] User jdoe logged in successfully",
        line);
  }
}
