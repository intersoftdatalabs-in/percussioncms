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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSSearchField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for pure helpers on {@link PSSearchDialog} after rawtypes cleanup (#2993). Does
 * not open the Swing dialog (requires live applet resources).
 */
public class PSSearchDialogTest {

  @Test
  public void collectSearchFieldNamesNullIteratorReturnsEmpty() {
    Set<String> names = PSSearchDialog.collectSearchFieldNames(null);
    assertTrue(names.isEmpty());
  }

  @Test
  public void collectSearchFieldNamesEmptyIteratorReturnsEmpty() {
    Set<String> names = PSSearchDialog.collectSearchFieldNames(Collections.emptyIterator());
    assertTrue(names.isEmpty());
  }

  @Test
  public void collectSearchFieldNamesCollectsDistinctNames() {
    List<PSSearchField> fields = new ArrayList<>();
    fields.add(field("sys_title"));
    fields.add(field("sys_contentid"));
    fields.add(field("sys_title")); // duplicate name — Set collapses

    Set<String> names = PSSearchDialog.collectSearchFieldNames(fields.iterator());
    assertEquals(2, names.size());
    assertTrue(names.contains("sys_title"));
    assertTrue(names.contains("sys_contentid"));
  }

  @Test
  public void buildDisplayFormatIdNameMapNullIteratorReturnsEmpty() {
    Map<String, String> map = PSSearchDialog.buildDisplayFormatIdNameMap(null);
    assertTrue(map.isEmpty());
  }

  @Test
  public void buildDisplayFormatIdNameMapEmptyIteratorReturnsEmpty() {
    Map<String, String> map =
        PSSearchDialog.buildDisplayFormatIdNameMap(Collections.emptyIterator());
    assertTrue(map.isEmpty());
  }

  @Test
  public void buildDisplayFormatIdNameMapMapsIdToDisplayName() throws PSCmsException {
    PSDisplayFormat format = new PSDisplayFormat();
    format.setDisplayName("Default Format");
    // Default ctor leaves display id as -1 until persisted
    Map<String, String> map =
        PSSearchDialog.buildDisplayFormatIdNameMap(Collections.singletonList(format).iterator());
    assertEquals(1, map.size());
    assertEquals("Default Format", map.get(Integer.toString(format.getDisplayId())));
  }

  @Test
  public void findDisallowedSynonymExpansionCharsNullReturnsNull() {
    assertNull(PSSearchDialog.findDisallowedSynonymExpansionChars(null));
  }

  @Test
  public void findDisallowedSynonymExpansionCharsPlainTextReturnsNull() {
    assertNull(PSSearchDialog.findDisallowedSynonymExpansionChars("simple query text"));
  }

  @Test
  public void findDisallowedSynonymExpansionCharsFindsSpecials() {
    // Order follows ms_specialChars static init: + then - then && ...
    assertEquals("+", PSSearchDialog.findDisallowedSynonymExpansionChars("a+b"));
    assertEquals("*", PSSearchDialog.findDisallowedSynonymExpansionChars("star*"));
    String multi = PSSearchDialog.findDisallowedSynonymExpansionChars("a+b*c");
    assertTrue(multi.contains("+"));
    assertTrue(multi.contains("*"));
  }

  private static PSSearchField field(String name) {
    return new PSSearchField(name, name, null, PSSearchField.TYPE_TEXT, null);
  }
}
