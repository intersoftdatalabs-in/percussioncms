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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for typed {@link PSSearchFieldFilter} keyword lists (#2386 / epic #2022).
 */
public class PSSearchFieldFilterTypedTest {

  @Test
  public void replaceFilterReturnsFilterKeywordsOnly() {
    PSEntry a = new PSEntry("1", "One");
    PSEntry b = new PSEntry("2", "Two");
    PSEntry c = new PSEntry("3", "Three");
    List<PSEntry> filterKeys = Arrays.asList(a, b);
    List<PSEntry> source = Arrays.asList(b, c);

    PSSearchFieldFilter filter =
        new PSSearchFieldFilter(
            "sys_contenttypeid", filterKeys, PSSearchFieldFilter.SEARCH_FILTER_TYPE_REPLACE);

    List<PSEntry> result = filter.getFilteredList(source);
    assertEquals(2, result.size());
    assertTrue(result.contains(a));
    assertTrue(result.contains(b));
  }

  @Test
  public void intersectionFilterKeepsSharedEntries() {
    PSEntry a = new PSEntry("1", "One");
    PSEntry b = new PSEntry("2", "Two");
    PSEntry c = new PSEntry("3", "Three");
    List<PSEntry> filterKeys = new ArrayList<>(Arrays.asList(a, b));
    List<PSEntry> source = new ArrayList<>(Arrays.asList(b, c));

    PSSearchFieldFilter filter =
        new PSSearchFieldFilter(
            "sys_contenttypeid", filterKeys, PSSearchFieldFilter.SEARCH_FILTER_TYPE_INTERSECTION);

    List<PSEntry> result = filter.getFilteredList(source);
    assertEquals(1, result.size());
    assertEquals(b, result.get(0));
  }

  @Test
  public void unionFilterMergesWithoutDuplicates() {
    PSEntry a = new PSEntry("1", "One");
    PSEntry b = new PSEntry("2", "Two");
    PSEntry c = new PSEntry("3", "Three");
    List<PSEntry> filterKeys = new ArrayList<>(Arrays.asList(a, b));
    List<PSEntry> source = new ArrayList<>(Arrays.asList(b, c));

    PSSearchFieldFilter filter =
        new PSSearchFieldFilter(
            "sys_contenttypeid", filterKeys, PSSearchFieldFilter.SEARCH_FILTER_TYPE_UNION);

    List<PSEntry> result = filter.getFilteredList(source);
    assertEquals(3, result.size());
    assertTrue(result.contains(a));
    assertTrue(result.contains(b));
    assertTrue(result.contains(c));
  }

  @Test
  public void nullKeywordsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new PSSearchFieldFilter(
                "sys_contenttypeid", null, PSSearchFieldFilter.SEARCH_FILTER_TYPE_REPLACE));
  }

  @Test
  public void nullSourceListRejected() {
    PSSearchFieldFilter filter =
        new PSSearchFieldFilter(
            "sys_contenttypeid",
            Arrays.asList(new PSEntry("1", "One")),
            PSSearchFieldFilter.SEARCH_FILTER_TYPE_REPLACE);
    assertThrows(IllegalArgumentException.class, () -> filter.getFilteredList(null));
  }
}
