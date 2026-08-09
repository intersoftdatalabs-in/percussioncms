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

import com.intsof.percussioncms.auditlog.AuditLogId;
import com.intsof.percussioncms.auditlog.SystemErrorCode;

/**
 * Formats sequential {@code {}} placeholders (SLF4J-style) and builds the canonical audit line
 * {@code [MOD-####]-[logId] message}.
 */
public final class MessageTemplateFormatter {

  private MessageTemplateFormatter() {}

  /**
   * Replace sequential {@code {}} placeholders with string forms of {@code params}. Extra params
   * are ignored; missing params leave {@code {}} intact.
   */
  public static String format(String template, Object... params) {
    if (template == null) {
      return "";
    }
    if (params == null || params.length == 0) {
      return template;
    }
    StringBuilder out = new StringBuilder(template.length() + 32);
    int paramIndex = 0;
    int i = 0;
    while (i < template.length()) {
      if (i + 1 < template.length() && template.charAt(i) == '{' && template.charAt(i + 1) == '}') {
        if (paramIndex < params.length) {
          out.append(stringify(params[paramIndex++]));
        } else {
          out.append("{}");
        }
        i += 2;
      } else {
        out.append(template.charAt(i));
        i++;
      }
    }
    return out.toString();
  }

  /** Canonical presentation: {@code [PUB-1001]-[uuid] body}. */
  public static String formatLine(SystemErrorCode code, AuditLogId logId, String messageBody) {
    String body = messageBody == null ? "" : messageBody;
    return "[" + code.qualifiedCode() + "]-[" + logId.value() + "] " + body;
  }

  private static String stringify(Object value) {
    if (value == null) {
      return "null";
    }
    return String.valueOf(value);
  }
}
