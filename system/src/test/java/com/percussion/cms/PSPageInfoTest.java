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
package com.percussion.cms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for PSPageInfo class. */
class PSPageInfoTest {

  private PSPageInfo pageInfo;
  private List<String> queryHandlers;
  private List<Integer> pageMap;

  @BeforeEach
  void setUp() {
    queryHandlers = new ArrayList<>();
    queryHandlers.add("dataset1");
    queryHandlers.add("dataset2");

    pageMap = new ArrayList<>();
    pageMap.add(1);
    pageMap.add(2);
    pageMap.add(3);
  }

  @Test
  void testConstructor_ValidParameters() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, pageMap);

    assertNotNull(pageInfo);
    assertEquals(100, pageInfo.getChildId());
  }

  @Test
  void testConstructor_InvalidType() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PSPageInfo(999, 100, queryHandlers, pageMap),
        "Should throw IllegalArgumentException for invalid type");
  }

  @Test
  void testConstructor_NullQueryHandlers() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, null, pageMap);

    assertNotNull(pageInfo);
    Iterator<String> datasetList = pageInfo.getDatasetList();
    assertNotNull(datasetList);
    assertFalse(datasetList.hasNext());
  }

  @Test
  void testConstructor_NullPageMap() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, null);

    assertNotNull(pageInfo);
    Iterator<Integer> pageIdList = pageInfo.getPageIdList();
    assertNotNull(pageIdList);
    assertFalse(pageIdList.hasNext());
  }

  @Test
  void testGetPageIdList_ReturnsCorrectType() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, pageMap);

    Iterator<Integer> pageIdList = pageInfo.getPageIdList();

    assertNotNull(pageIdList);
    assertTrue(pageIdList.hasNext());

    // Verify the page IDs are returned in correct order
    assertEquals(1, pageIdList.next());
    assertEquals(2, pageIdList.next());
    assertEquals(3, pageIdList.next());
    assertFalse(pageIdList.hasNext());
  }

  @Test
  void testGetPageIdList_EmptyPageMap() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, new ArrayList<>());

    Iterator<Integer> pageIdList = pageInfo.getPageIdList();

    assertNotNull(pageIdList);
    assertFalse(pageIdList.hasNext());
  }

  @Test
  void testGetDatasetList() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, pageMap);

    Iterator<String> datasetList = pageInfo.getDatasetList();

    assertNotNull(datasetList);
    assertTrue(datasetList.hasNext());
    assertEquals("dataset1", datasetList.next());
    assertEquals("dataset2", datasetList.next());
    assertFalse(datasetList.hasNext());
  }

  @Test
  void testIsSummaryEditor() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_SUMMARY_EDITOR, 100, queryHandlers, pageMap);

    assertTrue(pageInfo.isSummaryEditor());
    assertFalse(pageInfo.isRowEditor());
  }

  @Test
  void testIsRowEditor() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, pageMap);

    assertTrue(pageInfo.isRowEditor());
    assertFalse(pageInfo.isSummaryEditor());
  }

  @Test
  void testIsType() {
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, 100, queryHandlers, pageMap);

    assertTrue(pageInfo.isType(PSPageInfo.TYPE_ROW_EDITOR));
    assertFalse(pageInfo.isType(PSPageInfo.TYPE_SUMMARY_EDITOR));
  }

  @Test
  void testGetChildId() {
    int expectedChildId = 42;
    pageInfo = new PSPageInfo(PSPageInfo.TYPE_ROW_EDITOR, expectedChildId, queryHandlers, pageMap);

    assertEquals(expectedChildId, pageInfo.getChildId());
  }
}
