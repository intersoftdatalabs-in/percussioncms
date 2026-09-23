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
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * EditorHost managed-link / page-link field checks (#4753). Blank clears the field. A target is a
 * content id, a hyphenated content GUID, or a site folder path. This class does not create or
 * reorder slot relationships.
 */
public final class PSItemEditorLink {

  private static final Pattern CONTENT_ID = Pattern.compile("[0-9]{1,18}");
  private static final Pattern CONTENT_GUID = Pattern.compile("[0-9]+-[0-9]+-[0-9]+");
  private static final Pattern SITE_PATH =
      Pattern.compile("//[A-Za-z0-9._~-]+(?:/[A-Za-z0-9._~-]+)+");
  private static final Pattern FOLDER_PATH =
      Pattern.compile("/(?:[A-Za-z0-9._~-]+/)*[A-Za-z0-9._~-]+");

  private PSItemEditorLink() {}

  public static boolean isLinkDataType(String dataType) {
    return "link".equals(kind(dataType));
  }

  public static String normalize(String value) {
    return value == null ? "" : value.trim();
  }

  /** Path targets are resolved with {@code findItem}; ids use {@code findItemById}. */
  public static boolean isPath(String value) {
    String text = normalize(value);
    return text.startsWith("/");
  }

  /**
   * @return a rejection message when {@code dataType} is {@code link} and the value is not a
   *     clear, an id, or a folder path; otherwise {@code null}
   */
  public static String syntaxRejection(String fieldName, String value, String dataType) {
    if (!isLinkDataType(dataType)) {
      return null;
    }
    String text = normalize(value);
    if (text.isEmpty() || isContentId(text) || isFolderPath(text)) {
      return null;
    }
    String name = StringUtils.defaultString(fieldName);
    return String.format("Field \"%s\" is not a valid page or managed link.", name);
  }

  public static String notFoundMessage(String fieldName) {
    return String.format(
        "Field \"%s\" target was not found.", StringUtils.defaultString(fieldName));
  }

  public static String forbiddenMessage(String fieldName) {
    return String.format(
        "You are not allowed to use the target of field \"%s\".",
        StringUtils.defaultString(fieldName));
  }

  static boolean isContentId(String text) {
    return CONTENT_ID.matcher(text).matches() || CONTENT_GUID.matcher(text).matches();
  }

  static boolean isFolderPath(String text) {
    if (text.contains("..") || text.indexOf('\\') >= 0 || text.indexOf(' ') >= 0) {
      return false;
    }
    if (text.startsWith("//")) {
      return SITE_PATH.matcher(text).matches();
    }
    return FOLDER_PATH.matcher(text).matches();
  }

  private static String kind(String dataType) {
    return StringUtils.defaultString(dataType).trim().toLowerCase(Locale.ROOT);
  }
}
