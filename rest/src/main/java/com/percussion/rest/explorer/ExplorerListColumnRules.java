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
package com.percussion.rest.explorer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates Explorer folder-list column selections (#4722). Invalid input is an {@link
 * IllegalArgumentException} (HTTP 400). A missing principal is not validated here.
 */
public final class ExplorerListColumnRules {

  public static final String SYS_TITLE = "sys_title";

  /** Same CX sources the Explorer column picker offers. */
  public static final Set<String> ALLOWED_SOURCES =
      Set.of(
          "sys_title",
          "sys_checkoutstatus",
          "sys_statename",
          "sys_contenttypename",
          "sys_contentcreatedby",
          "sys_contentcreateddate",
          "sys_contentlastmodifieddate",
          "sys_contentid",
          "sys_workflow",
          "sys_postdate",
          "sys_size",
          "sys_locale",
          "sys_communityid",
          "sys_checkoutuser");

  public static final int MAX_COLUMNS = 16;

  private ExplorerListColumnRules() {}

  public static String normalizeFolderPath(String folderPath) {
    if (folderPath == null || folderPath.isBlank()) {
      throw new IllegalArgumentException("folderPath is required");
    }
    String path = folderPath.trim().replace('\\', '/');
    if (path.indexOf('\0') >= 0 || path.contains("..")) {
      throw new IllegalArgumentException("folderPath is invalid");
    }
    if (path.length() > 1024) {
      throw new IllegalArgumentException("folderPath is too long");
    }
    return path;
  }

  /**
   * @return canonical sources in request order (lower-case catalog keys)
   */
  public static List<String> normalizeColumns(List<String> columns) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException("columns are required");
    }
    if (columns.size() > MAX_COLUMNS) {
      throw new IllegalArgumentException("too many columns");
    }
    List<String> out = new ArrayList<>();
    boolean sawTitle = false;
    for (String raw : columns) {
      if (raw == null || raw.isBlank()) {
        throw new IllegalArgumentException("column source is required");
      }
      String key = raw.trim().toLowerCase(Locale.ROOT);
      if (!ALLOWED_SOURCES.contains(key)) {
        throw new IllegalArgumentException("unknown column source: " + key);
      }
      if (out.contains(key)) {
        throw new IllegalArgumentException("duplicate column source: " + key);
      }
      if (SYS_TITLE.equals(key)) {
        sawTitle = true;
      }
      out.add(key);
    }
    if (!sawTitle) {
      throw new IllegalArgumentException("sys_title is required");
    }
    return List.copyOf(out);
  }
}
