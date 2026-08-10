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

package com.percussion.services.assembly.data;

import com.percussion.services.assembly.IPSTemplateSlot;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Versioned schema helpers for slot {@code slot_layout} and {@code slot_styles} maps (ADR-003 /
 * Phase 2 #2629).
 *
 * <p>Stored form is a JSON object with integer {@link #KEY_SCHEMA_VERSION} plus string property
 * values (CM1-parity property set first: layout structure keys and style class tokens such as
 * {@code rootclass}).
 *
 * <p><b>Assembly / JEXL binding names</b> (when a slot context is established):
 *
 * <ul>
 *   <li>{@code $sys.slot.layout} — structural layout map for the current slot
 *   <li>{@code $sys.slot.styles} — presentational styles map for the current slot
 *   <li>{@code $sys.slot.schemaVersion} — integer schema version of the maps
 *   <li>{@code $sys.currentslot.slot.slotLayout} / {@code .slotStyles} — same maps via the slot
 *       definition object (Velocity AA macros)
 *   <li>{@code $rx.asmhelper.slotAssemblyContext(slot)} — full context map ({@code layout}, {@code
 *       styles}, {@code schemaVersion}, {@code name})
 * </ul>
 *
 * @see IPSTemplateSlot#getSlotLayout()
 * @see IPSTemplateSlot#getSlotStyles()
 */
public final class PSSlotLayoutStyles {

  private static final Logger log = LogManager.getLogger(PSSlotLayoutStyles.class);

  /** Current on-disk / API schema version for layout and styles maps. */
  public static final int SCHEMA_VERSION = 1;

  /** Reserved key holding the integer schema version inside each map. */
  public static final String KEY_SCHEMA_VERSION = "schemaVersion";

  // --- layout property keys (structural) ---
  public static final String KEY_ORIENTATION = "orientation";
  public static final String KEY_COLUMNS = "columns";
  public static final String KEY_MAX_ITEMS = "maxItems";
  public static final String KEY_EMPTY_STATE = "emptyState";
  public static final String KEY_WRAPPER_CLASS_POLICY = "wrapperClassPolicy";

  // --- styles property keys (CM1 CssPref parity first) ---
  public static final String KEY_ROOTCLASS = "rootclass";
  public static final String KEY_ITEMCLASS = "itemclass";

  /** Assembly context map key for layout (under {@code $sys.slot}). */
  public static final String CTX_LAYOUT = "layout";

  /** Assembly context map key for styles (under {@code $sys.slot}). */
  public static final String CTX_STYLES = "styles";

  /** Assembly context map key for schema version. */
  public static final String CTX_SCHEMA_VERSION = "schemaVersion";

  /** Assembly context map key for slot name. */
  public static final String CTX_NAME = "name";

  private static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();

  private static final TypeReference<Map<String, Object>> MAP_TYPE =
      new TypeReference<Map<String, Object>>() {};

  private PSSlotLayoutStyles() {}

  /**
   * Default layout map for a new or empty slot definition: schema version only (no structural
   * overrides).
   *
   * @return mutable map, never {@code null}
   */
  public static Map<String, Object> defaultLayout() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
    return m;
  }

  /**
   * Default styles map for a new or empty slot definition: schema version only (no class tokens).
   *
   * @return mutable map, never {@code null}
   */
  public static Map<String, Object> defaultStyles() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
    return m;
  }

  /**
   * Parse a stored JSON layout string into a mutable map. Blank or invalid JSON yields defaults.
   *
   * @param json may be {@code null} or blank
   * @return mutable map including {@link #KEY_SCHEMA_VERSION}, never {@code null}
   */
  public static Map<String, Object> parseLayout(String json) {
    return normalize(parseJson(json), true);
  }

  /**
   * Parse a stored JSON styles string into a mutable map. Blank or invalid JSON yields defaults.
   *
   * @param json may be {@code null} or blank
   * @return mutable map including {@link #KEY_SCHEMA_VERSION}, never {@code null}
   */
  public static Map<String, Object> parseStyles(String json) {
    return normalize(parseJson(json), false);
  }

  /**
   * Encode a layout map to JSON for persistence. {@code null} or empty → {@code null} storage
   * (defaults apply on read).
   *
   * @param layout may be {@code null}
   * @return JSON text or {@code null}
   * @throws IllegalStateException if Jackson cannot serialize the map (callers must not assume
   *     silent clear-to-null on failure)
   */
  public static String encodeLayout(Map<String, Object> layout) {
    return encode(layout, true);
  }

  /**
   * Encode a styles map to JSON for persistence. {@code null} or empty → {@code null} storage.
   *
   * @param styles may be {@code null}
   * @return JSON text or {@code null}
   * @throws IllegalStateException if Jackson cannot serialize the map
   */
  public static String encodeStyles(Map<String, Object> styles) {
    return encode(styles, false);
  }

  /**
   * Validate raw JSON intended for {@code SLOT_LAYOUT} / {@code SLOT_STYLES} storage.
   *
   * <p>Blank or {@code null} is allowed (means defaults on read). Non-blank text must parse as a
   * JSON object. On read, {@link #parseLayout(String)} / {@link #parseStyles(String)} still
   * soft-fail to defaults for pre-existing corrupt rows loaded via field access that bypasses
   * setters.
   *
   * @param json may be {@code null} or blank
   * @throws IllegalArgumentException if non-blank and not a JSON object
   */
  public static void validateStoredJson(String json) {
    if (StringUtils.isBlank(json)) {
      return;
    }
    try {
      Map<String, Object> raw = MAPPER.readValue(json.trim(), MAP_TYPE);
      if (raw == null) {
        throw new IllegalArgumentException("slot layout/styles JSON must be a JSON object");
      }
    } catch (JacksonException e) {
      throw new IllegalArgumentException(
          "Invalid slot layout/styles JSON: " + e.getMessage(), e);
    }
  }

  /**
   * Build the assembly context map for a slot definition (JEXL/Velocity binding under {@code
   * $sys.slot}).
   *
   * @param slot never {@code null}
   * @return unmodifiable view of a map with {@code layout}, {@code styles}, {@code schemaVersion},
   *     {@code name}; never {@code null}
   */
  public static Map<String, Object> toAssemblyContext(IPSTemplateSlot slot) {
    Objects.requireNonNull(slot, "slot");
    return toAssemblyContext(slot, null, null);
  }

  /**
   * Build assembly context merging composition-level instance overrides over slot definition
   * defaults. Override map keys win; absent override keys keep definition values. Pass {@code
   * null}/empty override maps to use definition only.
   *
   * @param slot never {@code null}
   * @param layoutOverrides sparse layout overrides (may be {@code null})
   * @param stylesOverrides sparse styles overrides (may be {@code null})
   * @return unmodifiable context map; never {@code null}
   */
  public static Map<String, Object> toAssemblyContext(
      IPSTemplateSlot slot,
      Map<String, Object> layoutOverrides,
      Map<String, Object> stylesOverrides) {
    Objects.requireNonNull(slot, "slot");
    Map<String, Object> layout = merge(slot.getSlotLayout(), layoutOverrides, true);
    Map<String, Object> styles = merge(slot.getSlotStyles(), stylesOverrides, false);
    Map<String, Object> ctx = new LinkedHashMap<>();
    ctx.put(CTX_LAYOUT, Collections.unmodifiableMap(layout));
    ctx.put(CTX_STYLES, Collections.unmodifiableMap(styles));
    ctx.put(CTX_SCHEMA_VERSION, SCHEMA_VERSION);
    ctx.put(CTX_NAME, slot.getName());
    return Collections.unmodifiableMap(ctx);
  }

  /**
   * Merge definition (base) maps with composition-level instance overrides. Instance keys win.
   * {@link #KEY_SCHEMA_VERSION} is always set to the max of base/override versions (or {@link
   * #SCHEMA_VERSION}). Null or empty overrides return a normalized copy of base.
   *
   * <p>To <em>clear</em> an override for a key, remove that key from the override map (do not put
   * {@code null} — null values are ignored). After clear, the definition value shows through.
   *
   * @param base definition defaults (may be {@code null} → empty defaults)
   * @param overrides sparse instance overrides (may be {@code null})
   * @param layout {@code true} for layout maps, {@code false} for styles
   * @return new mutable effective map, never {@code null}
   */
  public static Map<String, Object> merge(
      Map<String, Object> base, Map<String, Object> overrides, boolean layout) {
    Map<String, Object> out = layout ? defaultLayout() : defaultStyles();
    putNonSchema(out, base);
    putNonSchema(out, overrides);
    int ver = Math.max(schemaVersionOf(base), schemaVersionOf(overrides));
    out.put(KEY_SCHEMA_VERSION, ver >= 1 ? ver : SCHEMA_VERSION);
    return out;
  }

  /**
   * Remove a property from a sparse instance-override map so the definition default applies again
   * on the next {@link #merge}. Does not modify definition maps. No-op if map or key is null, or
   * key is absent.
   *
   * @param overrides mutable override map (may be {@code null})
   * @param key property to clear (may be {@code null})
   * @return the same map instance, or {@code null} if overrides was {@code null}
   */
  public static Map<String, Object> clearOverride(Map<String, Object> overrides, String key) {
    if (overrides == null || key == null) {
      return overrides;
    }
    overrides.remove(key);
    return overrides;
  }

  /**
   * Encode a sparse instance-override map for composition storage. Empty / null / schema-only →
   * {@code null} (no override stored).
   *
   * @param overrides may be {@code null}
   * @param layout {@code true} for layout, {@code false} for styles
   * @return JSON text or {@code null}
   */
  public static String encodeOverrides(Map<String, Object> overrides, boolean layout) {
    if (overrides == null || overrides.isEmpty()) {
      return null;
    }
    // Reuse encode path: schema-only collapses to null.
    return layout ? encodeLayout(overrides) : encodeStyles(overrides);
  }

  /**
   * Parse a sparse instance-override JSON blob. Blank/invalid → empty mutable map (no overrides).
   * Unlike {@link #parseLayout}, does not inject defaults other than schema version when the blob
   * is non-blank — for blank input returns an empty map (caller merges onto definition).
   *
   * @param json may be {@code null} or blank
   * @return mutable map, never {@code null} (empty when no overrides)
   */
  public static Map<String, Object> parseOverrides(String json) {
    Map<String, Object> raw = parseJson(json);
    if (raw == null || raw.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : raw.entrySet()) {
      if (e.getKey() == null || e.getValue() == null) {
        continue;
      }
      if (KEY_SCHEMA_VERSION.equals(e.getKey())) {
        out.put(KEY_SCHEMA_VERSION, schemaVersionOf(raw));
      } else {
        out.put(e.getKey(), e.getValue());
      }
    }
    return out;
  }

  private static void putNonSchema(Map<String, Object> out, Map<String, Object> src) {
    if (src == null || src.isEmpty()) {
      return;
    }
    for (Map.Entry<String, Object> e : src.entrySet()) {
      if (e.getKey() == null || KEY_SCHEMA_VERSION.equals(e.getKey())) {
        continue;
      }
      if (e.getValue() != null) {
        out.put(e.getKey(), e.getValue());
      }
    }
  }

  /**
   * Extract integer schema version from a map, defaulting to {@link #SCHEMA_VERSION}.
   *
   * @param map may be {@code null}
   * @return version &gt;= 1
   */
  public static int schemaVersionOf(Map<String, Object> map) {
    if (map == null || !map.containsKey(KEY_SCHEMA_VERSION)) {
      return SCHEMA_VERSION;
    }
    Object v = map.get(KEY_SCHEMA_VERSION);
    if (v instanceof Number n) {
      int i = n.intValue();
      return i >= 1 ? i : SCHEMA_VERSION;
    }
    if (v != null) {
      try {
        int i = Integer.parseInt(v.toString().trim());
        return i >= 1 ? i : SCHEMA_VERSION;
      } catch (NumberFormatException ignored) {
        return SCHEMA_VERSION;
      }
    }
    return SCHEMA_VERSION;
  }

  private static Map<String, Object> parseJson(String json) {
    if (StringUtils.isBlank(json)) {
      return null;
    }
    try {
      Map<String, Object> raw = MAPPER.readValue(json.trim(), MAP_TYPE);
      if (raw == null || raw.isEmpty()) {
        return null;
      }
      return new LinkedHashMap<>(raw);
    } catch (JacksonException e) {
      log.warn("Invalid slot layout/styles JSON; using defaults. Error: {}", e.getMessage());
      log.debug(e);
      return null;
    }
  }

  private static Map<String, Object> normalize(Map<String, Object> raw, boolean layout) {
    Map<String, Object> out = layout ? defaultLayout() : defaultStyles();
    if (raw == null || raw.isEmpty()) {
      return out;
    }
    for (Map.Entry<String, Object> e : raw.entrySet()) {
      if (e.getKey() == null) {
        continue;
      }
      if (KEY_SCHEMA_VERSION.equals(e.getKey())) {
        out.put(KEY_SCHEMA_VERSION, schemaVersionOf(raw));
      } else if (e.getValue() != null) {
        out.put(e.getKey(), e.getValue());
      }
    }
    // defaultLayout()/defaultStyles() always stamp KEY_SCHEMA_VERSION into out before the loop.
    return out;
  }

  private static String encode(Map<String, Object> map, boolean layout) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    Map<String, Object> normalized = normalize(new LinkedHashMap<>(map), layout);
    // Only schemaVersion with no other keys → store null (defaults on read).
    if (normalized.size() == 1 && normalized.containsKey(KEY_SCHEMA_VERSION)) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(normalized);
    } catch (JacksonException e) {
      // Do not return null here — callers would clear stored JSON without knowing encode failed.
      throw new IllegalStateException("Failed to encode slot layout/styles JSON", e);
    }
  }
}
