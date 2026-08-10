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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unified slot composition node produced from a CM1 region (tree sketch for Phase 2 residual
 * #2690). Not a persisted entity — upgrade/runtime mappers and offline tests use this DTO.
 *
 * <p>Mapping rule: {@code regionId} → {@link #getSlotName()}; nested regions → nested slot
 * composition; widgets on the region → ordered {@link #getItems()}.
 */
public final class PSSlotCompositionNode {

  private final String slotName;
  private final String sourceRegionId;
  private final String regionCssClass;
  private final Map<String, Object> slotLayout;
  private final Map<String, Object> slotStyles;
  private final List<PSSlotCompositionItem> items;
  private final List<PSSlotCompositionNode> children;

  /**
   * @param slotName never blank
   * @param sourceRegionId never blank
   * @param regionCssClass may be {@code null}
   * @param slotLayout never {@code null} (copied)
   * @param slotStyles never {@code null} (copied)
   * @param items never {@code null} (copied)
   * @param children never {@code null} (copied)
   */
  public PSSlotCompositionNode(
      String slotName,
      String sourceRegionId,
      String regionCssClass,
      Map<String, Object> slotLayout,
      Map<String, Object> slotStyles,
      List<PSSlotCompositionItem> items,
      List<PSSlotCompositionNode> children) {
    this.slotName = Objects.requireNonNull(slotName, "slotName");
    this.sourceRegionId = Objects.requireNonNull(sourceRegionId, "sourceRegionId");
    this.regionCssClass = regionCssClass;
    this.slotLayout =
        Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(slotLayout)));
    this.slotStyles =
        Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(slotStyles)));
    this.items = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(items)));
    this.children =
        Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(children)));
  }

  public String getSlotName() {
    return slotName;
  }

  public String getSourceRegionId() {
    return sourceRegionId;
  }

  public String getRegionCssClass() {
    return regionCssClass;
  }

  public Map<String, Object> getSlotLayout() {
    return slotLayout;
  }

  public Map<String, Object> getSlotStyles() {
    return slotStyles;
  }

  public List<PSSlotCompositionItem> getItems() {
    return items;
  }

  public List<PSSlotCompositionNode> getChildren() {
    return children;
  }

  /** Flatten this node and all descendants in preorder. */
  public List<PSSlotCompositionNode> flattenPreorder() {
    List<PSSlotCompositionNode> out = new ArrayList<>();
    flattenPreorder(this, out);
    return out;
  }

  private static void flattenPreorder(PSSlotCompositionNode node, List<PSSlotCompositionNode> out) {
    out.add(node);
    for (PSSlotCompositionNode child : node.children) {
      flattenPreorder(child, out);
    }
  }
}
