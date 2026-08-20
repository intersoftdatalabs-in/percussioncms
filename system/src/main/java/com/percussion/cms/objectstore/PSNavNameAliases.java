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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
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

  /** {@code percNavImage} or {@code rffNavImage} (any case). */
  public static boolean isNavImageTypeName(String typeName) {
    return "navimage".equals(navRoleKey(typeName));
  }

  /** True for perc/rff NavTree, Navon, or Nav Image type names. */
  public static boolean isNavTypeName(String typeName) {
    return isNavTreeTypeName(typeName)
        || isNavonTypeName(typeName)
        || isNavImageTypeName(typeName);
  }

  /**
   * FastForward sample-site installer ids ({@code rffNavImage} / {@code rffNavon} / {@code
   * rffNavTree}).
   */
  public static final long RFF_NAV_IMAGE_TYPE_ID = 313L;

  public static final long RFF_NAVON_TYPE_ID = 314L;

  public static final long RFF_NAV_TREE_TYPE_ID = 315L;

  /**
   * {@code perc.nav} package ids ({@code percNavImage} / {@code percNavon} / {@code percNavTree}).
   */
  public static final long PERC_NAV_IMAGE_TYPE_ID = 1015L;

  public static final long PERC_NAVON_TYPE_ID = 1016L;

  public static final long PERC_NAV_TREE_TYPE_ID = 1017L;

  /**
   * Role key for the well-known FastForward / perc.nav type ids when the catalog has no name for
   * that id. Empty when {@code typeId} is not one of those ids.
   */
  public static String wellKnownNavRole(long typeId) {
    if (typeId == RFF_NAV_IMAGE_TYPE_ID || typeId == PERC_NAV_IMAGE_TYPE_ID) {
      return "navimage";
    }
    if (typeId == RFF_NAVON_TYPE_ID || typeId == PERC_NAVON_TYPE_ID) {
      return "navon";
    }
    if (typeId == RFF_NAV_TREE_TYPE_ID || typeId == PERC_NAV_TREE_TYPE_ID) {
      return "navtree";
    }
    return "";
  }

  /**
   * When a FastForward {@code rffNav*} type (313–315) is missing from the JCR configuration map but
   * a {@code percNav*} sibling is registered (1015–1017), or the inverse, return the registered
   * alias id. Both pairs share {@code RXS_CT_NAV*} tables, so items of the missing id load with the
   * alias mapping. Name lookup may return null when ItemDef dropped the FastForward catalog entry;
   * well-known ids still match by role. Returns {@code null} when the missing id is not a nav type
   * or no sibling is registered.
   */
  public static Long findRegisteredNavAliasTypeId(
      long missingTypeId,
      Function<Long, String> typeIdToName,
      Collection<Long> registeredTypeIds) {
    if (typeIdToName == null || registeredTypeIds == null || registeredTypeIds.isEmpty()) {
      return null;
    }
    String missingRole = navRoleForType(missingTypeId, typeIdToName);
    if (missingRole.isEmpty()) {
      return null;
    }
    for (Long registered : registeredTypeIds) {
      if (registered == null || registered.longValue() == missingTypeId) {
        continue;
      }
      if (missingRole.equals(navRoleForType(registered, typeIdToName))) {
        return registered;
      }
    }
    return null;
  }

  /**
   * Catalog name role, else well-known FastForward / perc.nav id role. Never {@code null}; empty
   * when neither source identifies a nav type.
   */
  static String navRoleForType(long typeId, Function<Long, String> typeIdToName) {
    String fromName = navRoleKey(typeNameQuietly(typeIdToName, typeId));
    if (!fromName.isEmpty()) {
      return fromName;
    }
    return wellKnownNavRole(typeId);
  }

  private static String typeNameQuietly(Function<Long, String> typeIdToName, long typeId) {
    try {
      return typeIdToName.apply(typeId);
    } catch (RuntimeException e) {
      return null;
    }
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
