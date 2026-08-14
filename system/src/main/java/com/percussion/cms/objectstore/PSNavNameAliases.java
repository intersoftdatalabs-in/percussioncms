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
package com.percussion.cms.objectstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 * perc* and rff* names (and their {@code psx_ce*} content-editor apps) are the same Managed Nav
 * roles. Catalog, config, and CE registration must treat them as aliases.
 */
public final class PSNavNameAliases {

  private PSNavNameAliases() {}

  /**
   * @return true if both identify the same nav type or CE app (percNavTree vs rffNavTree,
   *     psx_cepercNavTree vs psx_cerffNavTree, etc.)
   */
  public static boolean sameNavRole(String left, String right) {
    String a = navRoleKey(left);
    String b = navRoleKey(right);
    return !a.isEmpty() && a.equals(b);
  }

  /**
   * Content-editor app name vs registered editor URL ({@code ../psx_cerffNavTree/rffNavTree.html}).
   */
  public static boolean sameNavEditor(String appName, String editorUrl) {
    if (StringUtils.isBlank(appName) || StringUtils.isBlank(editorUrl)) {
      return false;
    }
    try {
      return sameNavRole(appName, PSContentType.getAppName(editorUrl));
    } catch (IllegalArgumentException e) {
      return sameNavRole(appName, editorUrl);
    }
  }

  /** {@code percNavTree} or {@code rffNavTree} (any case). */
  public static boolean isNavTreeTypeName(String typeName) {
    return "navtree".equals(navRoleKey(typeName));
  }

  /** {@code percNavon} or {@code rffNavon} (any case). */
  public static boolean isNavonTypeName(String typeName) {
    return "navon".equals(navRoleKey(typeName));
  }

  /**
   * Split a {@code Navigation.properties} comma/semicolon list. Empty tokens dropped. Never {@code
   * null}.
   */
  public static List<String> splitConfiguredNames(String raw) {
    List<String> names = new ArrayList<>();
    if (raw == null || raw.isBlank()) {
      return names;
    }
    for (String part : raw.split("[,;]")) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        names.add(trimmed);
      }
    }
    return names;
  }

  /**
   * Stable role key: {@code psx_cepercNavTree} / {@code percNavTree} / {@code rffNavTree} → {@code
   * navtree}.
   */
  static String navRoleKey(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw.trim();
    if (s.isEmpty()) {
      return "";
    }
    s = s.replace('\\', '/');
    int slash = s.lastIndexOf('/');
    if (slash >= 0) {
      s = s.substring(slash + 1);
    }
    if (s.toLowerCase(Locale.ROOT).endsWith(".html")) {
      s = s.substring(0, s.length() - 5);
    }
    s = s.toLowerCase(Locale.ROOT);
    if (s.startsWith("psx_ce")) {
      s = s.substring("psx_ce".length());
    }
    if (s.startsWith("perc")) {
      s = s.substring("perc".length());
    } else if (s.startsWith("rff")) {
      s = s.substring("rff".length());
    }
    return s;
  }
}
