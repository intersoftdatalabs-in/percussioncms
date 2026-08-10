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

import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionNode;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSRegionWidgetAssociations;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Maps a CM1 {@link PSRegionTree} (structure + region↔widget associations) onto a unified slot
 * composition tree ({@link PSSlotCompositionNode}) for Phase 2 residual #2690.
 *
 * <h2>Mapping rules (documented + unit-tested)</h2>
 *
 * <ol>
 *   <li>Each {@link PSRegion} becomes one slot composition node; {@code regionId} → {@code
 *       slotName}.
 *   <li>Nested region children become nested slot composition children (code nodes are ignored for
 *       structure).
 *   <li>Region {@code cssClass} seeds {@code slot_styles.rootclass} when non-blank.
 *   <li>Widget items on a region (from associations) become ordered {@link PSSlotCompositionItem}s
 *       with instance layout/style overrides from {@link PSWidgetPrefToSlotMapper}.
 *   <li>Optional widget definitions supply slot definition defaults merged under region styles
 *       (definition defaults first, then region cssClass, then first-item merges are not applied at
 *       the slot level — items keep their own overrides).
 * </ol>
 *
 * <p>This is a pure offline sketch: no Hibernate, REST, or package I/O.
 *
 * @see PSWidgetPrefToSlotMapper
 */
public final class PSRegionToSlotCompositionMapper {

  private PSRegionToSlotCompositionMapper() {}

  /**
   * Map a region tree and its widget associations to a unified slot composition root.
   *
   * @param tree never {@code null}; must have a root region
   * @return composition root for {@link PSRegionTree#getRootRegion()}, never {@code null}
   * @throws IllegalArgumentException if tree or root is missing
   */
  public static PSSlotCompositionNode map(PSRegionTree tree) {
    return map(tree, Collections.emptyMap());
  }

  /**
   * Map a region tree with optional widget definitions for definition-level layout/style defaults
   * (used when attaching items; not required for structure).
   *
   * @param tree never {@code null}
   * @param definitionsById map of widget definition id → definition; may be empty, never {@code
   *     null}
   * @return composition root, never {@code null}
   */
  public static PSSlotCompositionNode map(
      PSRegionTree tree, Map<String, PSWidgetDefinition> definitionsById) {
    Objects.requireNonNull(tree, "tree");
    Objects.requireNonNull(definitionsById, "definitionsById");
    PSRegion root = tree.getRootRegion();
    if (root == null) {
      throw new IllegalArgumentException("region tree root is required");
    }
    Map<String, List<PSWidgetItem>> widgetsByRegion = tree.getRegionWidgetsMap();
    return mapRegion(root, widgetsByRegion, definitionsById);
  }

  /**
   * Map a single region subtree with associations supplied separately (for tests or non-tree
   * callers).
   *
   * @param region never {@code null}
   * @param associations may be {@code null} (no widgets)
   * @param definitionsById may be {@code null} (treated empty)
   * @return composition node for {@code region}, never {@code null}
   */
  public static PSSlotCompositionNode mapRegion(
      PSRegion region,
      PSRegionWidgetAssociations associations,
      Map<String, PSWidgetDefinition> definitionsById) {
    Objects.requireNonNull(region, "region");
    Map<String, List<PSWidgetItem>> widgetsByRegion =
        associations == null
            ? Collections.emptyMap()
            : associations.getRegionWidgetsMap();
    Map<String, PSWidgetDefinition> defs =
        definitionsById == null ? Collections.emptyMap() : definitionsById;
    return mapRegion(region, widgetsByRegion, defs);
  }

  private static PSSlotCompositionNode mapRegion(
      PSRegion region,
      Map<String, List<PSWidgetItem>> widgetsByRegion,
      Map<String, PSWidgetDefinition> definitionsById) {
    String regionId = region.getRegionId();
    if (StringUtils.isBlank(regionId)) {
      throw new IllegalArgumentException("regionId is required on every region");
    }

    Map<String, Object> layout = PSSlotLayoutStyles.defaultLayout();
    Map<String, Object> styles = PSSlotLayoutStyles.defaultStyles();
    if (StringUtils.isNotBlank(region.getCssClass())) {
      styles.put(PSSlotLayoutStyles.KEY_ROOTCLASS, region.getCssClass().trim());
    }

    List<PSWidgetItem> widgets =
        widgetsByRegion.getOrDefault(regionId, Collections.emptyList());
    List<PSSlotCompositionItem> items = new ArrayList<>();
    for (PSWidgetItem widget : widgets) {
      if (widget == null) {
        continue;
      }
      Map<String, Object> itemLayout = PSWidgetPrefToSlotMapper.instanceLayoutOverrides(widget);
      Map<String, Object> itemStyles = PSWidgetPrefToSlotMapper.instanceStyleOverrides(widget);
      String defId = widget.getDefinitionId();
      if (StringUtils.isNotBlank(defId) && definitionsById.containsKey(defId)) {
        PSWidgetDefinition def = definitionsById.get(defId);
        itemLayout =
            PSWidgetPrefToSlotMapper.merge(
                PSWidgetPrefToSlotMapper.definitionLayoutDefaults(def), itemLayout, true);
        itemStyles =
            PSWidgetPrefToSlotMapper.merge(
                PSWidgetPrefToSlotMapper.definitionStyleDefaults(def), itemStyles, false);
      }
      items.add(
          new PSSlotCompositionItem(widget.getId(), defId, itemLayout, itemStyles));
    }

    List<PSSlotCompositionNode> children = new ArrayList<>();
    List<PSRegionNode> childNodes = region.getChildren();
    if (childNodes != null) {
      for (PSRegionNode node : childNodes) {
        if (node instanceof PSRegion childRegion) {
          children.add(mapRegion(childRegion, widgetsByRegion, definitionsById));
        }
        // PSRegionCode nodes are template markup between regions — not slots.
      }
    }

    return new PSSlotCompositionNode(
        regionId, regionId, region.getCssClass(), layout, styles, items, children);
  }

  /**
   * Collect a flat regionId → slotName map for upgrade tooling (identity mapping under the Phase 2
   * sketch).
   *
   * @param root never {@code null}
   * @return linked map in preorder; never {@code null}
   */
  public static Map<String, String> regionIdToSlotNameMap(PSSlotCompositionNode root) {
    Objects.requireNonNull(root, "root");
    Map<String, String> map = new LinkedHashMap<>();
    for (PSSlotCompositionNode node : root.flattenPreorder()) {
      map.put(node.getSourceRegionId(), node.getSlotName());
    }
    return map;
  }
}
