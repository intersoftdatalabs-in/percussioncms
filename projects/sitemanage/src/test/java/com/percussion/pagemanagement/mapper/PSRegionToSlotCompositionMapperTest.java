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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.data.PSRegionTree;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetDefinition.CssPref;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Offline unit tests for CM1 region tree → unified slot composition mapping. */
@DisplayName("PSRegionToSlotCompositionMapper")
class PSRegionToSlotCompositionMapperTest {

  @Test
  void mapsRegionHierarchyToSlotNames() {
    PSRegion root = region("container", "perc-container");
    PSRegion header = region("header", null);
    PSRegion content = region("content", "main-content");
    root.getChildren().add(header);
    root.getChildren().add(newCode("<!-- sep -->"));
    root.getChildren().add(content);

    PSRegionTree tree = new PSRegionTree();
    tree.setRootRegion(root);

    PSSlotCompositionNode composition = PSRegionToSlotCompositionMapper.map(tree);
    assertEquals("container", composition.getSlotName());
    assertEquals("container", composition.getSourceRegionId());
    assertEquals(
        "perc-container", composition.getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals(2, composition.getChildren().size());
    assertEquals("header", composition.getChildren().get(0).getSlotName());
    assertEquals("content", composition.getChildren().get(1).getSlotName());
    assertEquals(
        "main-content",
        composition.getChildren().get(1).getSlotStyles().get(PSSlotLayoutStyles.KEY_ROOTCLASS));

    Map<String, String> idMap = PSRegionToSlotCompositionMapper.regionIdToSlotNameMap(composition);
    assertEquals(3, idMap.size());
    assertEquals("header", idMap.get("header"));
    assertTrue(composition.flattenPreorder().stream().noneMatch(n -> n.getSlotName().isBlank()));
  }

  @Test
  void mapsWidgetItemsWithInstanceOverrides() {
    PSRegion root = region("container", null);
    PSRegion content = region("content", null);
    root.getChildren().add(content);

    PSWidgetItem item = new PSWidgetItem();
    item.setId("-42");
    item.setDefinitionId("percRichText");
    Map<String, Object> css = new HashMap<>();
    css.put("rootclass", "page-widget-root");
    item.setCssProperties(css);
    Map<String, Object> props = new HashMap<>();
    props.put("layout", "ui-perc-list-horizontal");
    item.setProperties(props);

    PSRegionTree tree = new PSRegionTree();
    tree.setRootRegion(root);
    tree.setRegionWidgets("content", List.of(item));

    PSSlotCompositionNode composition = PSRegionToSlotCompositionMapper.map(tree);
    PSSlotCompositionNode contentNode = composition.getChildren().get(0);
    assertEquals(1, contentNode.getItems().size());
    PSSlotCompositionItem mapped = contentNode.getItems().get(0);
    assertEquals("-42", mapped.getWidgetInstanceId());
    assertEquals("percRichText", mapped.getDefinitionId());
    assertEquals(
        "page-widget-root", mapped.getStyleOverrides().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals(
        "horizontal", mapped.getLayoutOverrides().get(PSSlotLayoutStyles.KEY_ORIENTATION));
  }

  @Test
  void mergesWidgetDefinitionDefaultsUnderInstance() {
    PSRegion root = region("container", null);
    PSWidgetItem item = new PSWidgetItem();
    item.setId("-1");
    item.setDefinitionId("w1");
    Map<String, Object> css = new HashMap<>();
    css.put("rootclass", "instance");
    item.setCssProperties(css);

    PSRegionTree tree = new PSRegionTree();
    tree.setRootRegion(root);
    tree.setRegionWidgets("container", List.of(item));

    PSWidgetDefinition def = new PSWidgetDefinition();
    def.setId("w1");
    CssPref rootPref = new CssPref();
    rootPref.setName("rootclass");
    rootPref.setDefaultValue("definition-default");
    def.getCssPref().add(rootPref);
    CssPref sum = new CssPref();
    sum.setName("summaryclass");
    sum.setDefaultValue("def-summary");
    def.getCssPref().add(sum);

    Map<String, PSWidgetDefinition> defs = Map.of("w1", def);
    PSSlotCompositionNode composition = PSRegionToSlotCompositionMapper.map(tree, defs);
    PSSlotCompositionItem mapped = composition.getItems().get(0);
    assertEquals("instance", mapped.getStyleOverrides().get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("def-summary", mapped.getStyleOverrides().get("summaryclass"));
  }

  @Test
  void requiresRootRegion() {
    PSRegionTree tree = new PSRegionTree();
    assertThrows(IllegalArgumentException.class, () -> PSRegionToSlotCompositionMapper.map(tree));
  }

  private static PSRegion region(String id, String cssClass) {
    PSRegion r = new PSRegion();
    r.setRegionId(id);
    r.setCssClass(cssClass);
    return r;
  }

  private static PSRegionCode newCode(String templateCode) {
    PSRegionCode code = new PSRegionCode();
    code.setTemplateCode(templateCode);
    return code;
  }
}
