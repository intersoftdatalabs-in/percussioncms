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
package com.percussion.itemmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.itemmanagement.data.PSItemEditorField;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.share.dao.impl.PSContentItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class PSItemEditorFieldsMapperTest {

  @Test
  void omitsSystemFieldsExceptTitle() {
    assertTrue(PSItemEditorFieldsMapper.isEditableFieldName("sys_title"));
    assertTrue(PSItemEditorFieldsMapper.isEditableFieldName("displaytitle"));
    assertFalse(PSItemEditorFieldsMapper.isEditableFieldName("sys_contentid"));
    assertFalse(PSItemEditorFieldsMapper.isEditableFieldName("sys_workflowid"));
    assertFalse(PSItemEditorFieldsMapper.isEditableFieldName(""));
  }

  @Test
  void stringifiesScalarsAndSkipsBinary() {
    assertEquals("", PSItemEditorFieldsMapper.stringifyFieldValue(null));
    assertEquals("hello", PSItemEditorFieldsMapper.stringifyFieldValue("hello"));
    assertEquals("7", PSItemEditorFieldsMapper.stringifyFieldValue(7));
    assertEquals("true", PSItemEditorFieldsMapper.stringifyFieldValue(true));
    assertNull(PSItemEditorFieldsMapper.stringifyFieldValue(new byte[] {1, 2}));
    assertEquals("a", PSItemEditorFieldsMapper.stringifyFieldValue(List.of("a", "b")));
  }

  @Test
  void fromContentItemMapsEditableScalars() {
    PSContentItem item = new PSContentItem();
    item.setId("42");
    item.setType("percPage");
    item.setName("Home");
    Map<String, Object> fields = new HashMap<>();
    fields.put("sys_title", "Home");
    fields.put("sys_contentid", "42");
    fields.put("displaytitle", "Welcome");
    fields.put("body", new byte[] {1});
    item.setFields(fields);

    PSItemEditorFields out = PSItemEditorFieldsMapper.fromContentItem(item, "admin");
    assertEquals("42", out.getContentId());
    assertEquals("percPage", out.getContentType());
    assertEquals("Home", out.getName());
    assertEquals("admin", out.getCheckoutUser());
    assertEquals(2, out.getFields().size());
    assertEquals("displaytitle", out.getFields().get(0).getName());
    assertEquals("Welcome", out.getFields().get(0).getValue());
    assertEquals("sys_title", out.getFields().get(1).getName());
  }

  @Test
  void applyUpdatesIgnoresSystemExceptTitle() {
    PSContentItem item = new PSContentItem();
    Map<String, Object> fields = new HashMap<>();
    fields.put("sys_title", "Old");
    fields.put("sys_contentid", "42");
    fields.put("displaytitle", "A");
    item.setFields(fields);

    PSItemEditorFieldsMapper.applyUpdates(
        item,
        List.of(
            new PSItemEditorField("sys_title", "New"),
            new PSItemEditorField("sys_contentid", "99"),
            new PSItemEditorField("displaytitle", "B")));

    assertEquals("New", item.getFields().get("sys_title"));
    assertEquals("42", item.getFields().get("sys_contentid"));
    assertEquals("B", item.getFields().get("displaytitle"));
  }
}
