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
package com.percussion.itemmanagement.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 * EditorHost numeric field checks (#4752). Blank is allowed. {@code integer} and deprecated
 * {@code number} are whole numbers. {@code float} allows a fractional part. Inclusive {@code
 * minimum} / {@code maximum} are optional.
 */
public final class PSItemEditorNumeric {

  private PSItemEditorNumeric() {}

  public static boolean isNumericDataType(String dataType) {
    String kind = kind(dataType);
    return "integer".equals(kind) || "float".equals(kind);
  }

  /**
   * @return a field error naming {@code fieldName}, or {@code null} when the value may be stored
   */
  public static String rejectionMessage(
      String fieldName, String value, String dataType, String minimum, String maximum) {
    if (!isNumericDataType(dataType)) {
      return null;
    }
    String name = fieldName == null ? "" : fieldName;
    String text = value == null ? "" : value.trim();
    if (text.isEmpty()) {
      return null;
    }
    boolean integer = "integer".equals(kind(dataType));
    BigDecimal parsed = integer ? parseInteger(text) : parseDecimal(text);
    if (parsed == null) {
      return "Field \"" + name + "\" is not a valid number.";
    }
    BigDecimal low = bound(minimum);
    BigDecimal high = bound(maximum);
    if (low == null && high == null && integer) {
      low = new BigDecimal(Long.MIN_VALUE);
      high = new BigDecimal(Long.MAX_VALUE);
    }
    if ((low != null && parsed.compareTo(low) < 0) || (high != null && parsed.compareTo(high) > 0)) {
      return "Field \"" + name + "\" is outside the allowed range.";
    }
    return null;
  }

  private static String kind(String dataType) {
    String t = StringUtils.trimToEmpty(dataType).toLowerCase(Locale.ROOT);
    if ("integer".equals(t) || "number".equals(t)) {
      return "integer";
    }
    if ("float".equals(t)) {
      return "float";
    }
    return "";
  }

  private static BigDecimal parseInteger(String text) {
    if (!text.matches("-?\\d+")) {
      return null;
    }
    try {
      return new BigDecimal(new BigInteger(text));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static BigDecimal parseDecimal(String text) {
    if (!text.matches("-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)")) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static BigDecimal bound(String raw) {
    String text = StringUtils.trimToEmpty(raw);
    if (text.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
