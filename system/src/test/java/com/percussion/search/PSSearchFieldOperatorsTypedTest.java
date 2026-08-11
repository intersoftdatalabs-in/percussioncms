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
package com.percussion.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.design.objectstore.PSEntry;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSSearchFieldOperators} helpers (#2386 / epic #2022).
 */
public class PSSearchFieldOperatorsTypedTest {

  @Test
  public void getInputValuesStripsLikeWildcardsForTextFields() {
    PSSearchField field =
        new PSSearchField("sys_title", "Title", null, PSSearchField.TYPE_TEXT, "desc");
    field.setFieldValues(PSSearchField.OP_LIKE, Arrays.asList("%hello%", "%world", "foo%"));

    List<String> values = PSSearchFieldOperators.getInputValues(field);
    assertEquals(Arrays.asList("hello", "world", "foo"), values);
    assertEquals("hello", PSSearchFieldOperators.getInputValue(field));
  }

  @Test
  public void getInputValuesLeavesExactTextUnchanged() {
    PSSearchField field =
        new PSSearchField("sys_title", "Title", null, PSSearchField.TYPE_TEXT, "desc");
    field.setFieldValues(PSSearchField.OP_EQUALS, Arrays.asList("%keep%"));

    List<String> values = PSSearchFieldOperators.getInputValues(field);
    assertEquals(Arrays.asList("%keep%"), values);
  }

  @Test
  public void getOperatorsReturnsTypedEntriesForTextField() {
    PSSearchField field =
        new PSSearchField("sys_title", "Title", null, PSSearchField.TYPE_TEXT, "desc");
    Object[] ops = PSSearchFieldOperators.getOperators(field, null, "en-us");
    assertNotNull(ops);
    assertTrue(ops.length >= 4);
    for (Object op : ops) {
      assertTrue(op instanceof PSEntry);
      assertNotNull(((PSEntry) op).getValue());
    }
  }

  @Test
  public void getOutputValueWrapsContainsWildcard() {
    PSSearchField field =
        new PSSearchField("sys_title", "Title", null, PSSearchField.TYPE_TEXT, "desc");
    String out =
        PSSearchFieldOperators.getOutputValue("abc", PSCommonSearchUtils.OP_CONTAINS, field);
    assertEquals("%abc%", out);
  }

  @Test
  public void validateSearchFieldValueRejectsNonNumeric() {
    PSSearchField field =
        new PSSearchField("sys_contentid", "Id", null, PSSearchField.TYPE_NUMBER, "desc");
    field.setFieldValues(PSSearchField.OP_EQUALS, Arrays.asList("not-a-number"));
    String msg = PSSearchFieldOperators.validateSearchFieldValue(field, null, "en-us");
    assertNotNull(msg);
  }

  @Test
  public void validateSearchFieldValueAcceptsNumeric() {
    PSSearchField field =
        new PSSearchField("sys_contentid", "Id", null, PSSearchField.TYPE_NUMBER, "desc");
    field.setFieldValues(PSSearchField.OP_EQUALS, Arrays.asList("42"));
    assertNull(PSSearchFieldOperators.validateSearchFieldValue(field, null, "en-us"));
  }
}
