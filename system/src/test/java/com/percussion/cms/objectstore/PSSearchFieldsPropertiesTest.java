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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed {@link PSSearch} field/property iterators and property helpers
 * (rawtypes cleanup for issue #2349).
 */
public class PSSearchFieldsPropertiesTest {

  @Test
  public void getFieldsReturnsTypedIteratorOverAddedFields() throws Exception {
    PSSearch search = new PSSearch();
    PSSearchField field =
        new PSSearchField("sys_title", "Title", "", PSSearchField.TYPE_TEXT, "desc");
    search.addField(field);

    Iterator<PSSearchField> fields = search.getFields();
    assertTrue(fields.hasNext());
    PSSearchField next = fields.next();
    assertEquals("sys_title", next.getFieldName());
    assertFalse(fields.hasNext());
  }

  @Test
  public void setFieldsReplacesCollectionWithTypedIterator() throws Exception {
    PSSearch search = new PSSearch();
    search.addField(new PSSearchField("old_field", "Old", "", PSSearchField.TYPE_TEXT, ""));

    List<PSSearchField> replacements = new ArrayList<>();
    replacements.add(new PSSearchField("new_field", "New", "", PSSearchField.TYPE_NUMBER, ""));
    search.setFields(replacements.iterator());

    Iterator<PSSearchField> fields = search.getFields();
    assertTrue(fields.hasNext());
    assertEquals("new_field", fields.next().getFieldName());
    assertFalse(fields.hasNext());
  }

  @Test
  public void removeFieldsDropsMatchingFields() throws Exception {
    PSSearch search = new PSSearch();
    PSSearchField keep = new PSSearchField("keep_me", "Keep", "", PSSearchField.TYPE_TEXT, "");
    PSSearchField drop = new PSSearchField("drop_me", "Drop", "", PSSearchField.TYPE_TEXT, "");
    search.addField(keep);
    search.addField(drop);

    List<PSSearchField> toRemove = new ArrayList<>();
    toRemove.add(drop);
    search.removeFields(toRemove.iterator());

    List<String> names = new ArrayList<>();
    Iterator<PSSearchField> fields = search.getFields();
    while (fields.hasNext()) {
      names.add(fields.next().getFieldName());
    }
    assertTrue(names.contains("keep_me"));
    assertFalse(names.contains("drop_me"));
  }

  @Test
  public void propertyApisUseTypedIteratorsAndValues() throws Exception {
    PSSearch search = new PSSearch();
    search.setProperty("sys_community", "100");
    search.setProperty("sys_community", "200", true);

    assertTrue(search.hasProperty("sys_community"));
    assertTrue(search.doesPropertyHaveValue("sys_community", "100"));
    assertTrue(search.doesPropertyHaveValue("sys_community", "200"));

    String[] values = search.getPropertyValues("sys_community");
    assertEquals(2, values.length);

    Iterator<PSSearchMultiProperty> props = search.getProperties();
    boolean foundCommunity = false;
    while (props.hasNext()) {
      PSSearchMultiProperty prop = props.next();
      if ("sys_community".equalsIgnoreCase(prop.getName())) {
        foundCommunity = true;
        List<String> collected = new ArrayList<>();
        Iterator<String> propValues = prop.iterator();
        while (propValues.hasNext()) {
          collected.add(propValues.next());
        }
        assertTrue(collected.contains("100"));
        assertTrue(collected.contains("200"));
      }
    }
    assertTrue(foundCommunity, "expected typed getProperties to include sys_community");

    search.removeProperty("sys_community", "100", true);
    assertFalse(search.doesPropertyHaveValue("sys_community", "100"));
    assertTrue(search.doesPropertyHaveValue("sys_community", "200"));
  }
}
