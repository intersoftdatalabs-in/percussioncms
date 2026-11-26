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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.packages.pagexml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Slot {@code slot_layout} / {@code slot_styles} conformance for one slot (issue #7306 / parent
 * #2626).
 *
 * <p>Phase 2 (#2629) gave every manifest {@code SlotRef} {@code layout} / {@code styles} maps for
 * {@code slot_layout} / {@code slot_styles} (ADR-003), and region holes map into the slot model
 * ({@code PSPageXmlParser.extractRegionHoles} + {@code PSPageXmlCompiler}) — but no named check
 * states the contract, so unknown/typo'd layout keys, non-object style values, and slots that lost
 * their layout in the region mapping all pass silently. This helper is that check: given the
 * layout and styles maps of one slot it reports every conformance defect in document order.
 *
 * <p>Rules (mirroring what the pipeline already produces):
 *
 * <ul>
 *   <li>Layout keys must be known: the reserved {@code schemaVersion} key, the ADR-003 /
 *       {@code PSSlotLayoutStyles} CM1-parity structural set ({@code orientation}, {@code columns},
 *       {@code maxItems}, {@code emptyState}, {@code wrapperClassPolicy}), plus the region-mapper
 *       span hints ({@code vspan}, {@code hspan}) produced by {@code
 *       PSPageXmlParser.applyClassHints}. Any other layout key (including typos and blank keys)
 *       is reported in {@link Result#getUnknownLayoutKeys()} in first-encounter order.
 *   <li>Style keys are open: custom {@code CssPref} names are preserved by the upgrade mapping
 *       (for example {@code summaryclass}), so unknown style keys are never defects. Style
 *       <em>values</em> must be scalar ({@code String}, {@code Number}, {@code Boolean}, {@code
 *       Character}); {@code null}s, maps, collections, arrays, and other objects are reported in
 *       {@link Result#getNonScalarStyleKeys()} in first-encounter order.
 *   <li>Null or empty maps are informational only ({@link Result#isLayoutEmpty()} / {@link
 *       Result#isStylesEmpty()}) — a slot that lost its layout in the region mapping is surfaced,
 *       not failed. Null input is vacuously conformant (same convention as {@code
 *       PSPageXmlBindingConformance}).
 *   <li>The helper never sorts: every reported list preserves document (map iteration) order.
 * </ul>
 */
public final class PSSlotLayoutStylesConformance {

  /**
   * Layout keys the pipeline produces or the schema reserves. Any other layout key is reported as
   * unknown.
   */
  public static final Set<String> KNOWN_LAYOUT_KEYS =
      Set.of(
          "schemaVersion",
          "orientation",
          "columns",
          "maxItems",
          "emptyState",
          "wrapperClassPolicy",
          "vspan",
          "hspan");

  private PSSlotLayoutStylesConformance() {
    // utility
  }

  /**
   * Check the layout/styles maps of one slot.
   *
   * @param slotName slot name used for reporting (may be null/blank)
   * @param layout {@code slot_layout} map (may be null)
   * @param styles {@code slot_styles} map (may be null)
   * @return conformance result (never null)
   */
  public static Result check(
      String slotName, Map<String, Object> layout, Map<String, Object> styles) {
    List<String> layoutKeys = new ArrayList<>();
    List<String> unknownLayoutKeys = new ArrayList<>();
    if (layout != null) {
      for (String key : layout.keySet()) {
        layoutKeys.add(key);
        if (key == null || !KNOWN_LAYOUT_KEYS.contains(key)) {
          unknownLayoutKeys.add(key);
        }
      }
    }
    List<String> stylesKeys = new ArrayList<>();
    List<String> nonScalarStyleKeys = new ArrayList<>();
    if (styles != null) {
      for (Map.Entry<String, Object> entry : styles.entrySet()) {
        stylesKeys.add(entry.getKey());
        if (!isScalar(entry.getValue())) {
          nonScalarStyleKeys.add(entry.getKey());
        }
      }
    }
    return new Result(
        slotName,
        layoutKeys,
        stylesKeys,
        unknownLayoutKeys,
        nonScalarStyleKeys,
        layout == null || layout.isEmpty(),
        styles == null || styles.isEmpty());
  }

  private static boolean isScalar(Object value) {
    return value instanceof String
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Character;
  }

  /** Conformance result for one slot. Immutable. */
  public static final class Result {
    private final String slotName;
    private final List<String> layoutKeysInOrder;
    private final List<String> stylesKeysInOrder;
    private final List<String> unknownLayoutKeys;
    private final List<String> nonScalarStyleKeys;
    private final boolean layoutEmpty;
    private final boolean stylesEmpty;

    Result(
        String slotName,
        List<String> layoutKeysInOrder,
        List<String> stylesKeysInOrder,
        List<String> unknownLayoutKeys,
        List<String> nonScalarStyleKeys,
        boolean layoutEmpty,
        boolean stylesEmpty) {
      this.slotName = slotName;
      this.layoutKeysInOrder = Collections.unmodifiableList(new ArrayList<>(layoutKeysInOrder));
      this.stylesKeysInOrder = Collections.unmodifiableList(new ArrayList<>(stylesKeysInOrder));
      this.unknownLayoutKeys = Collections.unmodifiableList(new ArrayList<>(unknownLayoutKeys));
      this.nonScalarStyleKeys =
          Collections.unmodifiableList(new ArrayList<>(nonScalarStyleKeys));
      this.layoutEmpty = layoutEmpty;
      this.stylesEmpty = stylesEmpty;
    }

    /** Slot name used for reporting (may be null/blank). */
    public String getSlotName() {
      return slotName;
    }

    /** Layout keys in document order. The helper never sorts. */
    public List<String> getLayoutKeysInOrder() {
      return layoutKeysInOrder;
    }

    /** Style keys in document order. The helper never sorts. */
    public List<String> getStylesKeysInOrder() {
      return stylesKeysInOrder;
    }

    /** Unknown layout keys in first-encounter order. Empty when conformant. */
    public List<String> getUnknownLayoutKeys() {
      return unknownLayoutKeys;
    }

    /** Style keys with non-scalar values in first-encounter order. Empty when conformant. */
    public List<String> getNonScalarStyleKeys() {
      return nonScalarStyleKeys;
    }

    /**
     * Whether the layout map was null or empty. Informational only — an empty layout (for example
     * a slot that lost its layout in the region mapping) is surfaced, not failed.
     */
    public boolean isLayoutEmpty() {
      return layoutEmpty;
    }

    /** Whether the styles map was null or empty. Informational only. */
    public boolean isStylesEmpty() {
      return stylesEmpty;
    }

    /**
     * Whether the slot is conformant: no unknown layout keys and no non-scalar style values.
     * Empty maps do not affect conformance.
     */
    public boolean isConformant() {
      return unknownLayoutKeys.isEmpty() && nonScalarStyleKeys.isEmpty();
    }

    /** One-line human-readable summary naming the slot and every defect. */
    public String summarize() {
      StringBuilder summary = new StringBuilder();
      summary.append("slot '").append(slotName).append("' ");
      if (isConformant()) {
        summary.append("is conformant");
      } else {
        summary.append("is NOT conformant");
      }
      summary
          .append(" (layout keys: ")
          .append(layoutKeysInOrder.size())
          .append(", styles keys: ")
          .append(stylesKeysInOrder.size())
          .append(")");
      if (!unknownLayoutKeys.isEmpty()) {
        summary.append("; unknown layout keys: ").append(unknownLayoutKeys);
      }
      if (!nonScalarStyleKeys.isEmpty()) {
        summary.append("; non-scalar style values: ").append(nonScalarStyleKeys);
      }
      if (layoutEmpty) {
        summary.append("; layout empty (informational)");
      }
      if (stylesEmpty) {
        summary.append("; styles empty (informational)");
      }
      return summary.toString();
    }
  }
}
