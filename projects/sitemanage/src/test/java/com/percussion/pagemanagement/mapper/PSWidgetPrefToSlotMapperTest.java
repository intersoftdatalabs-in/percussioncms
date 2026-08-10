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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetDefinition.CssPref;
import com.percussion.pagemanagement.data.PSWidgetDefinition.UserPref;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.services.assembly.data.PSSlotLayoutStyles;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Offline unit tests for CssPref / UserPref → slot_layout / slot_styles mapping rules. */
@DisplayName("PSWidgetPrefToSlotMapper")
class PSWidgetPrefToSlotMapperTest {

  @Test
  void definitionStyleDefaultsFromCssPref() {
    PSWidgetDefinition def = new PSWidgetDefinition();
    CssPref root = new CssPref();
    root.setName("rootclass");
    root.setDefaultValue("perc-widget-root");
    def.getCssPref().add(root);
    CssPref item = new CssPref();
    item.setName("itemclass");
    item.setDefaultValue("perc-item");
    def.getCssPref().add(item);
    CssPref custom = new CssPref();
    custom.setName("summaryclass");
    custom.setDefaultValue("perc-summary");
    def.getCssPref().add(custom);

    Map<String, Object> styles = PSWidgetPrefToSlotMapper.definitionStyleDefaults(def);
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, styles.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
    assertEquals("perc-widget-root", styles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("perc-item", styles.get(PSSlotLayoutStyles.KEY_ITEMCLASS));
    assertEquals("perc-summary", styles.get("summaryclass"));
  }

  @Test
  void definitionStyleDefaultsIncludeLegacyRootclassUserPref() {
    PSWidgetDefinition def = new PSWidgetDefinition();
    UserPref root = new UserPref();
    root.setName("rootclass");
    root.setDefaultValue("legacy-root");
    def.getUserPref().add(root);

    Map<String, Object> styles = PSWidgetPrefToSlotMapper.definitionStyleDefaults(def);
    assertEquals("legacy-root", styles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
  }

  @Test
  void definitionLayoutDefaultsMapLayoutAndMaxlength() {
    PSWidgetDefinition def = new PSWidgetDefinition();
    UserPref layout = new UserPref();
    layout.setName("layout");
    layout.setDefaultValue("ui-perc-list-horizontal");
    def.getUserPref().add(layout);
    UserPref max = new UserPref();
    max.setName("maxlength");
    max.setDefaultValue("10");
    def.getUserPref().add(max);
    UserPref nonLayout = new UserPref();
    nonLayout.setName("target");
    nonLayout.setDefaultValue("_blank");
    def.getUserPref().add(nonLayout);

    Map<String, Object> layoutMap = PSWidgetPrefToSlotMapper.definitionLayoutDefaults(def);
    assertEquals("horizontal", layoutMap.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("10", layoutMap.get(PSSlotLayoutStyles.KEY_MAX_ITEMS));
    assertFalse(layoutMap.containsKey("target"));
  }

  @Test
  void instanceOverridesFromWidgetItem() {
    PSWidgetItem item = new PSWidgetItem();
    item.setId("-1");
    item.setDefinitionId("percRichText");
    Map<String, Object> css = new HashMap<>();
    css.put("rootclass", "instance-root");
    css.put("my_css", "extra");
    item.setCssProperties(css);
    Map<String, Object> props = new HashMap<>();
    props.put("layout", "ui-perc-list-vertical");
    props.put("maxlength", "3");
    props.put("target", "_self");
    item.setProperties(props);

    Map<String, Object> styles = PSWidgetPrefToSlotMapper.instanceStyleOverrides(item);
    assertEquals("instance-root", styles.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("extra", styles.get("my_css"));
    assertFalse(styles.containsKey(PSSlotLayoutStyles.KEY_ORIENTATION));

    Map<String, Object> layout = PSWidgetPrefToSlotMapper.instanceLayoutOverrides(item);
    assertEquals("vertical", layout.get(PSSlotLayoutStyles.KEY_ORIENTATION));
    assertEquals("3", layout.get(PSSlotLayoutStyles.KEY_MAX_ITEMS));
    assertFalse(layout.containsKey("target"));
  }

  @Test
  void mergeInstanceWinsOverDefinition() {
    Map<String, Object> base = PSSlotLayoutStyles.defaultStyles();
    base.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "def-root");
    base.put("summaryclass", "def-sum");
    Map<String, Object> over = PSSlotLayoutStyles.defaultStyles();
    over.put(PSSlotLayoutStyles.KEY_ROOTCLASS, "inst-root");

    Map<String, Object> merged = PSWidgetPrefToSlotMapper.merge(base, over, false);
    assertEquals("inst-root", merged.get(PSSlotLayoutStyles.KEY_ROOTCLASS));
    assertEquals("def-sum", merged.get("summaryclass"));
    assertEquals(PSSlotLayoutStyles.SCHEMA_VERSION, merged.get(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));
  }

  @Test
  void nullDefinitionAndItemYieldSchemaOnlyDefaults() {
    Map<String, Object> styles = PSWidgetPrefToSlotMapper.definitionStyleDefaults(null);
    Map<String, Object> layout = PSWidgetPrefToSlotMapper.definitionLayoutDefaults(null);
    assertEquals(1, styles.size());
    assertEquals(1, layout.size());
    assertTrue(styles.containsKey(PSSlotLayoutStyles.KEY_SCHEMA_VERSION));

    Map<String, Object> instStyles = PSWidgetPrefToSlotMapper.instanceStyleOverrides(null);
    Map<String, Object> instLayout = PSWidgetPrefToSlotMapper.instanceLayoutOverrides(null);
    assertEquals(1, instStyles.size());
    assertEquals(1, instLayout.size());
  }

  @Test
  void classifyPrefNames() {
    assertTrue(PSWidgetPrefToSlotMapper.isLayoutUserPref("Layout"));
    assertTrue(PSWidgetPrefToSlotMapper.isLayoutUserPref("max_results"));
    assertFalse(PSWidgetPrefToSlotMapper.isLayoutUserPref("sectionPath"));
    assertTrue(PSWidgetPrefToSlotMapper.isStyleUserPref("RootClass"));
    assertEquals(
        PSSlotLayoutStyles.KEY_ORIENTATION, PSWidgetPrefToSlotMapper.mapLayoutKey("layout"));
    assertEquals("rootclass", PSWidgetPrefToSlotMapper.normalizeStyleKey("RootClass"));
  }
}
