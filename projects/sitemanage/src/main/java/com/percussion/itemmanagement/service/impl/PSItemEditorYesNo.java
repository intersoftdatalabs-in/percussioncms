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

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 * EditorHost yes-no ({@code sys_SingleCheckBox}) checks (#5586 / #4532). Blank is allowed so an
 * optional clear still writes. Required-blank stays on the client required-field gate (#5587).
 */
public final class PSItemEditorYesNo {

  private PSItemEditorYesNo() {}

  public static boolean isYesNoDataType(String dataType) {
    String kind = kind(dataType);
    return "boolean".equals(kind);
  }

  /**
   * Canonical stored value: empty, {@code yes}, or {@code no}. {@code null} when the text cannot
   * be stored as a yes-no field.
   */
  public static String canonicalValue(String value) {
    String text = StringUtils.trimToEmpty(value);
    if (text.isEmpty()) {
      return "";
    }
    String lower = text.toLowerCase(Locale.ROOT);
    if ("yes".equals(lower)) {
      return "yes";
    }
    if ("no".equals(lower)) {
      return "no";
    }
    return null;
  }

  /**
   * @return a field error naming {@code fieldName}, or {@code null} when the value may be stored
   */
  public static String rejectionMessage(String fieldName, String value, String dataType) {
    if (!isYesNoDataType(dataType)) {
      return null;
    }
    if (canonicalValue(value) != null) {
      return null;
    }
    String name = fieldName == null ? "" : fieldName;
    return "Field '" + name + "' is not a valid yes-no value.";
  }

  private static String kind(String dataType) {
    String t = StringUtils.trimToEmpty(dataType).toLowerCase(Locale.ROOT);
    if ("boolean".equals(t) || "yesno".equals(t) || "yes-no".equals(t)) {
      return "boolean";
    }
    return "";
  }
}
