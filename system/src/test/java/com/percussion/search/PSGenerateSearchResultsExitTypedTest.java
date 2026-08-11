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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.search.PSGenerateSearchResultsExit.SearchField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed search-results exit parameter parsing (#2873 / epic #2022).
 */
public class PSGenerateSearchResultsExitTypedTest {

  @Test
  public void parseParametersSplitsSearchFieldsFromHtmlParams() {
    PSGenerateSearchResultsExit exit = new PSGenerateSearchResultsExit();
    Map<String, Object> request = new HashMap<>();
    request.put("sys_title_1", "hello");
    request.put("sys_title_op", "equals");
    request.put("sys_contentid", "42");
    request.put("otherParam", "keep");

    Map<String, SearchField> searchFields = new HashMap<>();
    Map<String, Object> html = exit.parseParameters(request.entrySet().iterator(), searchFields);

    assertTrue(searchFields.containsKey("sys_title"));
    SearchField title = searchFields.get("sys_title");
    PSSearchField textField = newTextField("sys_title");
    // operator was captured from *_op request param and is translated for the field
    assertTrue(title.getOperator(textField) != null && !title.getOperator(textField).isEmpty());
    List<String> values = title.getValues(textField);
    assertEquals(1, values.size());
    assertEquals("hello", values.get(0));

    assertEquals("42", html.get("sys_contentid").toString());
    assertEquals("keep", html.get("otherParam").toString());
    assertFalse(html.containsKey("sys_title_1"));
    assertFalse(html.containsKey("sys_title_op"));
  }

  @Test
  public void searchFieldAcceptsListValuesAtIndex() {
    PSGenerateSearchResultsExit exit = new PSGenerateSearchResultsExit();
    Map<String, Object> request = new HashMap<>();
    List<String> multi = new ArrayList<>(Arrays.asList("a", "b"));
    request.put("ctype_1", multi);
    request.put("ctype_op", "in");

    Map<String, SearchField> searchFields = new HashMap<>();
    exit.parseParameters(request.entrySet().iterator(), searchFields);

    SearchField field = searchFields.get("ctype");
    List<String> values = field.getValues(newTextField("ctype"));
    assertEquals(Arrays.asList("a", "b"), values);
  }

  @Test
  public void searchFieldAddValueAppendsTypedStrings() {
    SearchField field = new SearchField("sys_title");
    field.addValue("one");
    field.addValue("two");
    List<String> values = field.getValues(newTextField("sys_title"));
    assertEquals(Arrays.asList("one", "two"), values);
  }

  private static PSSearchField newTextField(String name) {
    return new PSSearchField(name, name, null, PSSearchField.TYPE_TEXT, "desc");
  }
}
