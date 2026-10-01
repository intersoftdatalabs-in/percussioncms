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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.percussion.cms.objectstore.server.util.PSFieldFinderUtilTest;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSUISet;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * External-link navon {@code no_externalurl} is a parent field. {@link PSItemDefExtractor} builds
 * parent fields with {@code isMultiValue} false, so {@link
 * com.percussion.cms.objectstore.PSItemField#addValue} already replaces the single value.
 * {@code clearValues} is required only for simple-child multi-value fields. Lookup must also
 * accept {@code no_externalUrl} (#4985).
 */
class PSCoreItemSetTextFieldTest {

  @Test
  void setTextFieldReplacesParentDelimitedField() throws Exception {
    // false matches PSItemDefExtractor parent fields, not TYPE_SIMPLE_CHILD.
    PSItemField field =
        new PSItemField(
            new PSField(PSField.TYPE_LOCAL, "no_externalurl", null), new PSUISet(), false);
    field.addValue(new PSTextValue(""));
    PSItemDefinition itemDef = PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    PSCoreItem item = new PSCoreItem(itemDef);
    Method addField = PSCoreItem.class.getDeclaredMethod("addField", PSItemField.class);
    addField.setAccessible(true);
    addField.invoke(item, field);

    assertEquals(false, field.isMultiValue());
    item.setTextField("no_externalurl", "https://example.com/edited");
    assertEquals(List.of("https://example.com/edited"), valueStrings(field));

    item.setTextField("no_externalurl", "https://example.com/again");
    assertEquals(List.of("https://example.com/again"), valueStrings(field));
  }

  @Test
  void setTextFieldReplacesSimpleChildMultiValue() throws Exception {
    PSItemField field =
        new PSItemField(new PSField(PSField.TYPE_LOCAL, "keywords", null), new PSUISet(), true);
    field.addValue(new PSTextValue(""));
    field.addValue(new PSTextValue("old"));
    PSItemDefinition itemDef = PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    PSCoreItem item = new PSCoreItem(itemDef);
    Method addField = PSCoreItem.class.getDeclaredMethod("addField", PSItemField.class);
    addField.setAccessible(true);
    addField.invoke(item, field);

    item.setTextField("keywords", "new");
    assertEquals(List.of("new"), valueStrings(field));
  }

  @Test
  void setTextFieldStillReplacesSingleValue() throws Exception {
    PSItemField field =
        new PSItemField(new PSField(PSField.TYPE_LOCAL, "displaytitle", null), new PSUISet(), false);
    field.addValue(new PSTextValue("Home"));
    PSItemDefinition def = PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    PSCoreItem item = new PSCoreItem(def);
    Method addField = PSCoreItem.class.getDeclaredMethod("addField", PSItemField.class);
    addField.setAccessible(true);
    addField.invoke(item, field);

    item.setTextField("displaytitle", "Edited");

    assertEquals(List.of("Edited"), valueStrings(field));
  }

  @Test
  void setTextFieldMatchesExternalUrlIgnoreCase() throws Exception {
    PSItemField field =
        new PSItemField(
            new PSField(PSField.TYPE_LOCAL, "no_externalUrl", null), new PSUISet(), false);
    field.addValue(new PSTextValue(""));
    PSItemDefinition itemDef = PSFieldFinderUtilTest.loadItemDefinition("PSFieldFinderUtilTest1.xml");
    PSCoreItem item = new PSCoreItem(itemDef);
    Method addField = PSCoreItem.class.getDeclaredMethod("addField", PSItemField.class);
    addField.setAccessible(true);
    addField.invoke(item, field);

    item.setTextField("no_externalurl", "https://example.com/edited");

    assertEquals(List.of("https://example.com/edited"), valueStrings(field));
  }

  private static List<String> valueStrings(PSItemField field) throws Exception {
    List<String> values = new ArrayList<>();
    Iterator<IPSFieldValue> it = field.getAllValues();
    while (it.hasNext()) {
      values.add(it.next().getValueAsString());
    }
    return values;
  }
}
