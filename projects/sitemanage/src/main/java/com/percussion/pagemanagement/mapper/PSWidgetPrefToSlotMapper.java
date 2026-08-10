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
package com.percussion.pagemanagement.mapper;

import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetDefinition.AbstractUserPref;
import com.percussion.pagemanagement.data.PSWidgetDefinition.CssPref;
import com.percussion.pagemanagement.data.PSWidgetDefinition.UserPref;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Offline-friendly upgrade/runtime sketch that maps CM1 widget {@code CssPref} / layout-ish {@code
 * UserPref} definitions and instance values onto unified {@code slot_layout} / {@code slot_styles}
 * maps (ADR-003 / Phase 2 residual #2690).
 *
 * <p>Does not touch the database or packages; callers apply the maps to slot definitions or
 * composition instances. REST/Workbench exposure is out of scope (#2691).
 *
 * @see PSRegionToSlotCompositionMapper
 * @see PSSlotLayoutStyles
 */
public final class PSWidgetPrefToSlotMapper {

  /**
   * UserPref names treated as structural layout (→ {@code slot_layout}). Case-insensitive match on
   * the pref name.
   */
  public static final Set<String> LAYOUT_USER_PREF_NAMES =
      Set.of(
          "layout",
          "orientation",
          "columns",
          "maxitems",
          "maxlength",
          "max_results",
          "maxresults",
          "emptystate",
          "wrapperclasspolicy");

  /**
   * UserPref names that are presentational style tokens even when declared as UserPref (legacy
   * widgets sometimes put {@code rootclass} here instead of CssPref).
   */
  public static final Set<String> STYLE_USER_PREF_NAMES = Set.of("rootclass", "itemclass");

  private PSWidgetPrefToSlotMapper() {}

  /**
   * Build slot definition {@code slot_styles} defaults from widget definition CssPref entries (and
   * style-ish UserPrefs with default values).
   *
   * @param definition may be {@code null} → empty defaults
   * @return mutable map including schema version; never {@code null}
   */
  public static Map<String, Object> definitionStyleDefaults(PSWidgetDefinition definition) {
    Map<String, Object> styles = PSSlotLayoutStyles.defaultStyles();
    if (definition == null) {
      return styles;
    }
    for (CssPref pref : definition.getCssPref()) {
      putPrefDefault(styles, normalizeStyleKey(pref.getName()), pref);
    }
    for (UserPref pref : definition.getUserPref()) {
      if (pref == null || pref.getName() == null) {
        continue;
      }
      if (isStyleUserPref(pref.getName())) {
        putPrefDefault(styles, normalizeStyleKey(pref.getName()), pref);
      }
    }
    return styles;
  }

  /**
   * Build slot definition {@code slot_layout} defaults from layout-ish UserPref default values.
   *
   * @param definition may be {@code null} → empty defaults
   * @return mutable map including schema version; never {@code null}
   */
  public static Map<String, Object> definitionLayoutDefaults(PSWidgetDefinition definition) {
    Map<String, Object> layout = PSSlotLayoutStyles.defaultLayout();
    if (definition == null) {
      return layout;
    }
    for (UserPref pref : definition.getUserPref()) {
      if (pref == null || pref.getName() == null) {
        continue;
      }
      if (!isLayoutUserPref(pref.getName())) {
        continue;
      }
      putPrefDefault(layout, mapLayoutKey(pref.getName()), pref);
    }
    return layout;
  }

  /**
   * Instance style overrides from a placed widget item ({@link PSWidgetItem#getCssProperties()}
   * plus style-ish keys in {@link PSWidgetItem#getProperties()}).
   *
   * @param item may be {@code null} → empty defaults (schema only)
   * @return mutable map including schema version; never {@code null}
   */
  public static Map<String, Object> instanceStyleOverrides(PSWidgetItem item) {
    Map<String, Object> styles = PSSlotLayoutStyles.defaultStyles();
    if (item == null) {
      return styles;
    }
    copyMappedValues(styles, item.getCssProperties(), true);
    copyMappedValues(styles, item.getProperties(), true);
    return styles;
  }

  /**
   * Instance layout overrides from a placed widget item's properties.
   *
   * @param item may be {@code null} → empty defaults (schema only)
   * @return mutable map including schema version; never {@code null}
   */
  public static Map<String, Object> instanceLayoutOverrides(PSWidgetItem item) {
    Map<String, Object> layout = PSSlotLayoutStyles.defaultLayout();
    if (item == null) {
      return layout;
    }
    copyMappedValues(layout, item.getProperties(), false);
    return layout;
  }

  /**
   * Merge base definition map with instance overrides. Instance keys win. Schema version is
   * stamped from {@link PSSlotLayoutStyles#SCHEMA_VERSION}.
   *
   * @param base may be {@code null}
   * @param overrides may be {@code null}
   * @param layout {@code true} for layout maps, {@code false} for styles
   * @return new mutable map; never {@code null}
   */
  public static Map<String, Object> merge(
      Map<String, Object> base, Map<String, Object> overrides, boolean layout) {
    Map<String, Object> out = layout ? PSSlotLayoutStyles.defaultLayout() : PSSlotLayoutStyles.defaultStyles();
    putNonSchema(out, base);
    putNonSchema(out, overrides);
    out.put(PSSlotLayoutStyles.KEY_SCHEMA_VERSION, PSSlotLayoutStyles.SCHEMA_VERSION);
    return out;
  }

  /**
   * Whether a UserPref / property name is considered layout-ish for upgrade mapping.
   *
   * @param name may be {@code null}
   * @return {@code true} if layout-mapped
   */
  public static boolean isLayoutUserPref(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return LAYOUT_USER_PREF_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Whether a UserPref name is treated as a style token (legacy rootclass-as-UserPref).
   *
   * @param name may be {@code null}
   * @return {@code true} if style-mapped from UserPref
   */
  public static boolean isStyleUserPref(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return STYLE_USER_PREF_NAMES.contains(name.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Map a CM1 layout UserPref name onto a {@link PSSlotLayoutStyles} layout key.
   *
   * @param prefName never {@code null} when used after {@link #isLayoutUserPref(String)}
   * @return canonical layout key
   */
  public static String mapLayoutKey(String prefName) {
    Objects.requireNonNull(prefName, "prefName");
    String n = prefName.trim().toLowerCase(Locale.ROOT);
    return switch (n) {
      case "layout", "orientation" -> PSSlotLayoutStyles.KEY_ORIENTATION;
      case "columns" -> PSSlotLayoutStyles.KEY_COLUMNS;
      case "maxitems", "maxlength", "max_results", "maxresults" -> PSSlotLayoutStyles.KEY_MAX_ITEMS;
      case "emptystate" -> PSSlotLayoutStyles.KEY_EMPTY_STATE;
      case "wrapperclasspolicy" -> PSSlotLayoutStyles.KEY_WRAPPER_CLASS_POLICY;
      default -> prefName.trim();
    };
  }

  /**
   * Normalize a style key. Known CM1 names are lowercased to match {@link
   * PSSlotLayoutStyles#KEY_ROOTCLASS} / {@link PSSlotLayoutStyles#KEY_ITEMCLASS}; other CssPref
   * names are preserved with trimmed original casing after lowercasing for rootclass/itemclass
   * only.
   *
   * @param name may be blank
   * @return normalized key or {@code null} if blank
   */
  public static String normalizeStyleKey(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    String trimmed = name.trim();
    String lower = trimmed.toLowerCase(Locale.ROOT);
    if (PSSlotLayoutStyles.KEY_ROOTCLASS.equals(lower)
        || PSSlotLayoutStyles.KEY_ITEMCLASS.equals(lower)) {
      return lower;
    }
    return trimmed;
  }

  /**
   * Known layout-ish property names that upgrade tools should scan in package widget XML (for
   * residual package upgrade documentation / inventory).
   *
   * @return unmodifiable set of lower-case names
   */
  public static Set<String> knownLayoutPrefNamesLower() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(LAYOUT_USER_PREF_NAMES));
  }

  private static void putPrefDefault(
      Map<String, Object> target, String key, AbstractUserPref pref) {
    if (key == null || pref == null) {
      return;
    }
    String def = pref.getDefaultValue();
    if (StringUtils.isBlank(def)) {
      // Presence of the pref still documents the allowed key for tools; skip empty defaults so
      // encode() can store null for schema-only maps.
      return;
    }
    target.put(key, coerceLayoutValue(key, def.trim()));
  }

  private static void copyMappedValues(
      Map<String, Object> target, Map<String, Object> source, boolean styles) {
    if (source == null || source.isEmpty()) {
      return;
    }
    for (Map.Entry<String, Object> e : source.entrySet()) {
      if (e.getKey() == null || e.getValue() == null) {
        continue;
      }
      String rawName = e.getKey().trim();
      if (rawName.isEmpty()) {
        continue;
      }
      if (styles) {
        // Css properties map is style-primary; also accept style-ish property keys.
        if (isLayoutUserPref(rawName) && !isStyleUserPref(rawName)) {
          continue;
        }
        String key = normalizeStyleKey(rawName);
        if (key != null) {
          target.put(key, stringValue(e.getValue()));
        }
      } else {
        if (!isLayoutUserPref(rawName)) {
          continue;
        }
        String key = mapLayoutKey(rawName);
        target.put(key, coerceLayoutValue(key, stringValue(e.getValue())));
      }
    }
  }

  private static void putNonSchema(Map<String, Object> out, Map<String, Object> source) {
    if (source == null) {
      return;
    }
    for (Map.Entry<String, Object> e : source.entrySet()) {
      if (e.getKey() == null
          || PSSlotLayoutStyles.KEY_SCHEMA_VERSION.equals(e.getKey())
          || e.getValue() == null) {
        continue;
      }
      out.put(e.getKey(), e.getValue());
    }
  }

  /**
   * Map CM1 list "layout" enum values (e.g. {@code ui-perc-list-horizontal}) onto orientation
   * tokens when the target key is orientation; otherwise keep the string.
   */
  private static Object coerceLayoutValue(String key, String value) {
    if (value == null) {
      return null;
    }
    if (PSSlotLayoutStyles.KEY_ORIENTATION.equals(key)) {
      String lower = value.toLowerCase(Locale.ROOT);
      if (lower.contains("horizontal")) {
        return "horizontal";
      }
      if (lower.contains("vertical")) {
        return "vertical";
      }
    }
    return value;
  }

  private static String stringValue(Object value) {
    return value == null ? null : value.toString();
  }
}
