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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSLocator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed accessors on {@link PSComponentSummaries} (rawtypes/unchecked cleanup
 * for issue #2296).
 */
public class PSComponentSummariesTest {

  private static PSComponentSummary item(int id, String name) {
    return new PSComponentSummary(id, 1, 1, 1, PSComponentSummary.TYPE_ITEM, name, 301, 0);
  }

  private static PSComponentSummary folder(int id, String name) {
    return new PSComponentSummary(id, 1, 1, 1, PSComponentSummary.TYPE_FOLDER, name, 101, 3);
  }

  @Test
  public void emptyDefaultCtor() {
    PSComponentSummaries summaries = new PSComponentSummaries();
    assertEquals(0, summaries.size());
    assertTrue(summaries.getLocators().isEmpty());
    assertTrue(summaries.getComponentList(PSComponentSummary.TYPE_ITEM).isEmpty());
    assertNull(summaries.getComponentFromId(1));
  }

  @Test
  public void arrayCtorAndToArrayRoundTrip() {
    PSComponentSummary a = item(10, "a");
    PSComponentSummary b = folder(20, "b");
    PSComponentSummaries summaries = new PSComponentSummaries(new PSComponentSummary[] {a, b});
    assertEquals(2, summaries.size());
    PSComponentSummary[] arr = summaries.toArray();
    assertEquals(2, arr.length);
    assertEquals(10, arr[0].getContentId());
    assertEquals(20, arr[1].getContentId());
  }

  @Test
  public void nullArrayCtorRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSComponentSummaries((PSComponentSummary[]) null));
  }

  @Test
  public void getComponentListFiltersByType() {
    PSComponentSummaries summaries =
        new PSComponentSummaries(
            new PSComponentSummary[] {item(1, "i1"), folder(2, "f1"), item(3, "i2")});
    List<PSComponentSummary> items = summaries.getComponentList(PSComponentSummary.TYPE_ITEM);
    List<PSComponentSummary> folders = summaries.getComponentList(PSComponentSummary.TYPE_FOLDER);
    assertEquals(2, items.size());
    assertEquals(1, folders.size());
    assertEquals("f1", folders.get(0).getName());
  }

  @Test
  public void getComponentNamesAndLocators() {
    PSComponentSummaries summaries =
        new PSComponentSummaries(new PSComponentSummary[] {item(5, "alpha"), folder(6, "beta")});

    List<String> itemNames = summaries.getComponentNames(PSComponentSummary.TYPE_ITEM);
    assertEquals(List.of("alpha"), itemNames);

    List<PSLocator> folderLocators =
        summaries.getComponentLocators(
            PSComponentSummary.TYPE_FOLDER, PSComponentSummary.GET_CURRENT_LOCATOR);
    assertEquals(1, folderLocators.size());
    assertEquals(6, folderLocators.get(0).getId());

    List<PSLocator> keyLocators =
        summaries.getComponentLocators(
            PSComponentSummary.TYPE_ITEM, PSComponentSummary.GET_LOCATOR);
    assertEquals(1, keyLocators.size());
    assertEquals(5, keyLocators.get(0).getId());

    List<PSLocator> allLocators = summaries.getLocators();
    assertEquals(2, allLocators.size());
  }

  @Test
  public void getComponentLocatorsRejectsUnsupportedLocatorType() {
    PSComponentSummaries summaries =
        new PSComponentSummaries(new PSComponentSummary[] {item(1, "x")});
    // GET_NAME and other non-locator constants must fail fast (typed List<PSLocator> API)
    assertThrows(
        IllegalArgumentException.class,
        () ->
            summaries.getComponentLocators(
                PSComponentSummary.TYPE_ITEM, PSComponentSummary.GET_NAME));
    assertThrows(
        IllegalArgumentException.class,
        () -> summaries.getComponentLocators(PSComponentSummary.TYPE_ITEM, 99));
  }

  @Test
  public void getComponentFromId() {
    PSComponentSummary target = item(42, "found");
    PSComponentSummaries summaries =
        new PSComponentSummaries(new PSComponentSummary[] {item(1, "x"), target, folder(2, "y")});
    assertSame(target, summaries.getComponentFromId(42));
    assertNull(summaries.getComponentFromId(999));
  }

  @Test
  public void invalidTypeRejected() {
    PSComponentSummaries summaries = new PSComponentSummaries();
    assertThrows(IllegalArgumentException.class, () -> summaries.getComponentList(99));
    assertThrows(IllegalArgumentException.class, () -> summaries.getComponentNames(-1));
  }

  @Test
  public void listElementCtorRejectsNonElements() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSComponentSummaries(Arrays.asList("not-an-element")));
  }
}
