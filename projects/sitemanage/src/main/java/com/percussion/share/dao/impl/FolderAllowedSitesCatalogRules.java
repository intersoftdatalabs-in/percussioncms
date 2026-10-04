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
package com.percussion.share.dao.impl;

import com.percussion.pathmanagement.data.PSFolderAllowedSiteChoice;
import com.percussion.pathmanagement.data.PSFolderAllowedSitesCatalog;
import com.percussion.services.sitemgr.IPSSite;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the folder allowed-publish-sites catalog and the canonical {@code sys_allowed_sites}
 * list. Site ids are positive decimals. An empty list clears the property so assets may publish
 * to all sites. A site name is not an id (#5132).
 */
public final class FolderAllowedSitesCatalogRules {
  public enum Kind {
    OMIT,
    CLEAR,
    LIST,
    INVALID
  }

  /**
   * Parsed wire value.
   *
   * @param kind how the raw value should be saved
   * @param canonical sorted unique site ids, or empty when the value is not a list
   * @param ids canonical ids in the same order as {@code canonical}
   */
  public record Parsed(Kind kind, String canonical, List<String> ids) {}

  private FolderAllowedSitesCatalogRules() {}

  public static Parsed parse(String raw) {
    if (raw == null) {
      return new Parsed(Kind.OMIT, "", List.of());
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return new Parsed(Kind.CLEAR, "", List.of());
    }
    List<Long> unique = new ArrayList<>();
    for (String part : trimmed.split(",", -1)) {
      String token = part == null ? "" : part.trim();
      if (token.isEmpty() || !digitsOnly(token)) {
        return new Parsed(Kind.INVALID, "", List.of());
      }
      long id;
      try {
        id = Long.parseLong(token);
      } catch (NumberFormatException ex) {
        return new Parsed(Kind.INVALID, "", List.of());
      }
      if (id <= 0 || unique.contains(id)) {
        if (id <= 0) {
          return new Parsed(Kind.INVALID, "", List.of());
        }
        continue;
      }
      unique.add(id);
    }
    unique.sort(Long::compare);
    List<String> ids = new ArrayList<>();
    for (Long id : unique) {
      ids.add(Long.toString(id));
    }
    return new Parsed(Kind.LIST, String.join(",", ids), List.copyOf(ids));
  }

  /** True when both strings are null or both trim to the same text. */
  public static boolean sameRaw(String left, String right) {
    if (left == null || right == null) {
      return left == right;
    }
    return left.trim().equals(right.trim());
  }

  /**
   * True when both values are the same site-id set. Null and blank are the same empty set. An
   * invalid value matches only the same trimmed text.
   */
  public static boolean sameList(String left, String right) {
    Parsed a = parse(left);
    Parsed b = parse(right);
    if (a.kind() == Kind.INVALID || b.kind() == Kind.INVALID) {
      return sameRaw(left, right);
    }
    return canonicalOf(a).equals(canonicalOf(b));
  }

  public static PSFolderAllowedSitesCatalog fromSites(List<IPSSite> sites) {
    List<PSFolderAllowedSiteChoice> raw = new ArrayList<>();
    if (sites != null) {
      for (IPSSite site : sites) {
        if (site == null || site.getSiteId() == null) {
          continue;
        }
        PSFolderAllowedSiteChoice choice = new PSFolderAllowedSiteChoice();
        choice.setId(Long.toString(site.getSiteId()));
        choice.setName(site.getName());
        raw.add(choice);
      }
    }
    return fromChoices(raw);
  }

  public static PSFolderAllowedSitesCatalog fromChoices(List<PSFolderAllowedSiteChoice> raw) {
    Map<String, PSFolderAllowedSiteChoice> byId = new LinkedHashMap<>();
    if (raw != null) {
      for (PSFolderAllowedSiteChoice choice : raw) {
        PSFolderAllowedSiteChoice cleaned = clean(choice);
        if (cleaned != null) {
          byId.putIfAbsent(cleaned.getId(), cleaned);
        }
      }
    }
    List<PSFolderAllowedSiteChoice> choices = new ArrayList<>(byId.values());
    choices.sort(
        Comparator.comparing(PSFolderAllowedSiteChoice::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PSFolderAllowedSiteChoice::getId, FolderAllowedSitesCatalogRules::compareIds));
    PSFolderAllowedSitesCatalog catalog = new PSFolderAllowedSitesCatalog();
    catalog.setChoices(choices);
    return catalog;
  }

  public static boolean contains(PSFolderAllowedSitesCatalog catalog, String siteId) {
    return nameOf(catalog, siteId) != null;
  }

  /** True when {@code canonical} is a non-empty list and every id is in the catalog. */
  public static boolean containsAll(PSFolderAllowedSitesCatalog catalog, String canonical) {
    Parsed parsed = parse(canonical);
    if (parsed.kind() != Kind.LIST) {
      return false;
    }
    for (String id : parsed.ids()) {
      if (!contains(catalog, id)) {
        return false;
      }
    }
    return true;
  }

  /** Display name for a catalog site id, or {@code null} when the id is not listed. */
  public static String nameOf(PSFolderAllowedSitesCatalog catalog, String siteId) {
    if (catalog == null || catalog.getChoices() == null) {
      return null;
    }
    Parsed wanted = parse(siteId);
    if (wanted.kind() != Kind.LIST || wanted.ids().size() != 1) {
      return null;
    }
    String id = wanted.ids().get(0);
    for (PSFolderAllowedSiteChoice choice : catalog.getChoices()) {
      if (choice != null && id.equals(choice.getId())) {
        String name = trim(choice.getName());
        return name.isEmpty() ? id : name;
      }
    }
    return null;
  }

  private static String canonicalOf(Parsed parsed) {
    return parsed.kind() == Kind.LIST ? parsed.canonical() : "";
  }

  private static PSFolderAllowedSiteChoice clean(PSFolderAllowedSiteChoice choice) {
    if (choice == null) {
      return null;
    }
    Parsed id = parse(choice.getId());
    if (id.kind() != Kind.LIST || id.ids().size() != 1) {
      return null;
    }
    String name = trim(choice.getName());
    if (name.isEmpty()) {
      return null;
    }
    PSFolderAllowedSiteChoice out = new PSFolderAllowedSiteChoice();
    out.setId(id.ids().get(0));
    out.setName(name);
    return out;
  }

  private static int compareIds(String left, String right) {
    try {
      return Long.compare(Long.parseLong(left), Long.parseLong(right));
    } catch (NumberFormatException ex) {
      return String.valueOf(left).compareTo(String.valueOf(right));
    }
  }

  private static boolean digitsOnly(String token) {
    for (int i = 0; i < token.length(); i++) {
      if (!Character.isDigit(token.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static String trim(String value) {
    return value == null ? "" : value.trim();
  }
}
