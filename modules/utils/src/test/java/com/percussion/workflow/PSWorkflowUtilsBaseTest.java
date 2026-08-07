/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the raw-type public API of {@link PSWorkflowUtilsBase}. The methods exercised here
 * are the same ones flagged by review thread PRRT_kwDOKZBp3M6XPfAi as needing to remain raw for
 * backward source compatibility with downstream modules and XML apps. These tests pass raw {@link
 * ArrayList}, {@link HashMap}, and {@link List} arguments to assert that the public API still
 * accepts those shapes.
 */
@Tag("UnitTest")
public class PSWorkflowUtilsBaseTest {

  private Properties m_savedProperties;

  @BeforeEach
  public void setUp() {
    // PSWorkflowUtilsBase.setTransitionCommentInHTMLParams reads
    // PSWorkflowUtilsBase.properties; save & initialize so tests don't NPE.
    m_savedProperties = PSWorkflowUtilsBase.properties;
    PSWorkflowUtilsBase.properties = new Properties();
  }

  @AfterEach
  public void tearDown() {
    PSWorkflowUtilsBase.properties = m_savedProperties;
  }

  @Test
  public void arrayToListAcceptsStringArray() {
    String[] input = {"alpha", "beta"};
    List result = PSWorkflowUtilsBase.arrayToList(input);
    assertEquals(2, result.size());
    assertEquals("alpha", result.get(0));
  }

  @Test
  public void arrayToListAcceptsIntArray() {
    int[] input = {1, 2, 3};
    List result = PSWorkflowUtilsBase.arrayToList(input);
    assertEquals(3, result.size());
    assertEquals(1, result.get(0));
  }

  @Test
  public void arrayToListOnNullReturnsEmptyList() {
    List result = PSWorkflowUtilsBase.arrayToList(null);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void compareRoleListThreeArgFindsAssignmentType() {
    ArrayList assignmentTypes = new ArrayList();
    assignmentTypes.add(1);
    assignmentTypes.add(2);
    assignmentTypes.add(3);
    ArrayList roleNames = new ArrayList();
    roleNames.add("Author");
    roleNames.add("Editor");
    roleNames.add("Admin");
    int result = PSWorkflowUtilsBase.compareRoleList(assignmentTypes, roleNames, "Editor,foo");
    assertEquals(2, result);
  }

  @Test
  public void compareRoleListThreeArgReturnsNoneForUnknown() {
    ArrayList assignmentTypes = new ArrayList();
    assignmentTypes.add(1);
    ArrayList roleNames = new ArrayList();
    roleNames.add("Author");
    int result =
        PSWorkflowUtilsBase.compareRoleList(assignmentTypes, roleNames, "UnknownRole,Another");
    assertEquals(PSWorkflowUtilsBase.ASSIGNMENT_TYPE_NONE, result);
  }

  @Test
  public void compareRoleListTwoArgFindsCommonRole() {
    List roleList = Arrays.asList("Author", "Editor", "Admin");
    assertTrue(PSWorkflowUtilsBase.compareRoleList(roleList, "Editor,foo"));
  }

  @Test
  public void compareRoleListTwoArgReturnsFalseWhenNoMatch() {
    List roleList = Arrays.asList("Author", "Editor");
    assertEquals(false, PSWorkflowUtilsBase.compareRoleList(roleList, "Admin,Reviewer"));
  }

  @Test
  public void caseInsensitiveUniqueListDedupesCaseInsensitively() {
    List input = Arrays.asList("alpha", "ALPHA", "Beta", "beta", "Gamma");
    List result = PSWorkflowUtilsBase.caseInsensitiveUniqueList(input);
    assertEquals(3, result.size());
    assertTrue(result.contains("alpha"));
    assertTrue(result.contains("Beta"));
    assertTrue(result.contains("Gamma"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void filterUserNameIsNoopPreservingCommas() {
    // Deprecated IP-security helper is intentionally a no-op (RX-02-11-0151).
    assertEquals("Lee, Christo", PSWorkflowUtilsBase.filterUserName("Lee, Christo"));
    assertEquals("admin", PSWorkflowUtilsBase.filterUserName("admin"));
    assertNull(PSWorkflowUtilsBase.filterUserName(null));
  }

  @Test
  public void caseInsensitiveUniqueListOnNullReturnsNull() {
    assertNull(PSWorkflowUtilsBase.caseInsensitiveUniqueList(null));
  }

  @Test
  public void caseInsensitiveUniqueListOnEmptyReturnsEmptyList() {
    List result = PSWorkflowUtilsBase.caseInsensitiveUniqueList(new ArrayList());
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void caseInsensitiveUniqueListTrimsAndDropsNulls() {
    List input = Arrays.asList("alpha", null, "  alpha  ");
    List result = PSWorkflowUtilsBase.caseInsensitiveUniqueList(input);
    assertEquals(1, result.size());
  }

  @Test
  public void intersectListsReturnsCommonElements() {
    List list1 = Arrays.asList("a", "b", "c");
    List list2 = Arrays.asList("b", "c", "d");
    List result = PSWorkflowUtilsBase.intersectLists(list1, list2);
    assertEquals(2, result.size());
    assertTrue(result.contains("b"));
    assertTrue(result.contains("c"));
  }

  @Test
  public void intersectListsOnEmptyReturnsEmpty() {
    List result = PSWorkflowUtilsBase.intersectLists(new ArrayList(), Arrays.asList("a"));
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void intersectListsOnNullReturnsNull() {
    assertNull(PSWorkflowUtilsBase.intersectLists(null, Arrays.asList("a")));
    assertNull(PSWorkflowUtilsBase.intersectLists(Arrays.asList("a"), null));
  }

  @Test
  public void getRoleIdFromMapFindsKey() {
    Map roleMap = new HashMap();
    roleMap.put(1, "Author");
    roleMap.put(2, "Editor");
    roleMap.put(3, "Admin");
    assertEquals(2, PSWorkflowUtilsBase.getRoleIdFromMap(roleMap, "Editor"));
  }

  @Test
  public void getRoleIdFromMapReturnsMinusOneForMissing() {
    Map roleMap = new HashMap();
    roleMap.put(1, "Author");
    assertEquals(-1, PSWorkflowUtilsBase.getRoleIdFromMap(roleMap, "Missing"));
  }

  @Test
  public void getRoleIdFromMapRejectsNullArgs() {
    Map roleMap = new HashMap();
    assertThrows(
        IllegalArgumentException.class, () -> PSWorkflowUtilsBase.getRoleIdFromMap(roleMap, null));
    assertThrows(
        IllegalArgumentException.class, () -> PSWorkflowUtilsBase.getRoleIdFromMap(null, "x"));
  }

  @Test
  public void listToDelimitedStringJoinsWithDelimiter() {
    List input = Arrays.asList("a", "b", "c");
    String result = PSWorkflowUtilsBase.listToDelimitedString(input, ",");
    assertEquals("a,b,c", result);
  }

  @Test
  public void listToDelimitedStringRejectsEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWorkflowUtilsBase.listToDelimitedString(new ArrayList(), ","));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWorkflowUtilsBase.listToDelimitedString((List) null, ","));
  }

  @Test
  public void listToDelimitedStringSingleElementHasNoDelimiter() {
    List input = Arrays.asList("only");
    assertEquals("only", PSWorkflowUtilsBase.listToDelimitedString(input, ","));
  }

  @Test
  public void listToDelimitedStringUsesStringForNullForNullEntries() {
    List input = Arrays.asList("a", null, "c");
    String result = PSWorkflowUtilsBase.listToDelimitedString(input, ",", "<null>");
    assertEquals("a,<null>,c", result);
  }

  @Test
  public void filterListReturnsMatchingEntries() {
    List input = Arrays.asList("a", "b", "c");
    Map filter = new HashMap();
    filter.put("a", Boolean.TRUE);
    filter.put("c", Boolean.TRUE);
    List result = PSWorkflowUtilsBase.filterList(input, filter);
    assertEquals(2, result.size());
    assertTrue(result.contains("a"));
    assertTrue(result.contains("c"));
  }

  @Test
  public void filterListOnEmptyMapReturnsNull() {
    assertNull(PSWorkflowUtilsBase.filterList(Arrays.asList("a"), new HashMap()));
  }

  @Test
  public void filterListOnNullInputReturnsNull() {
    Map filter = new HashMap();
    filter.put("a", Boolean.TRUE);
    assertNull(PSWorkflowUtilsBase.filterList(null, filter));
  }

  @Test
  public void filterListOnEmptyInputReturnsEmptyList() {
    Map filter = new HashMap();
    filter.put("a", Boolean.TRUE);
    List result = PSWorkflowUtilsBase.filterList(new ArrayList(), filter);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void setTransitionCommentInHTMLParamsAcceptsRawHashMap() {
    HashMap params = new HashMap();
    PSWorkflowUtilsBase.setTransitionCommentInHTMLParams("my comment", params);
    assertEquals("my comment", params.get(PSWorkflowUtilsBase.TRANSITION_COMMENT));
  }

  @Test
  public void setTransitionCommentInHTMLParamsRejectsLongComment() {
    HashMap params = new HashMap();
    StringBuilder tooLong = new StringBuilder();
    for (int i = 0; i < 256; i++) tooLong.append('x');
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWorkflowUtilsBase.setTransitionCommentInHTMLParams(tooLong.toString(), params));
  }

  @Test
  public void setTransitionCommentInHTMLParamsRejectsNullParams() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWorkflowUtilsBase.setTransitionCommentInHTMLParams("x", null));
  }

  @Test
  public void setTransitionCommentInHTMLParamsIgnoresNullComment() {
    HashMap params = new HashMap();
    PSWorkflowUtilsBase.setTransitionCommentInHTMLParams(null, params);
    assertTrue(params.isEmpty());
  }

  /**
   * Compile-time check that the raw public method signatures accept raw {@link List} (not
   * parameterized {@code List<String>}). Legacy callers constructed raw {@code List} locals and
   * passed them in; review thread PRRT_kwDOKZBp3M6XPfAi flagged that the parameterized signatures
   * broke that source-compat path. This test would not compile if the public method signatures were
   * tightened back to parameterized types.
   */
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void rawListSignaturesAreAcceptedByPublicAPI() {
    List rawList = new ArrayList();
    rawList.add("a");
    rawList.add("b");
    HashMap rawMap = new HashMap();
    rawMap.put("a", Boolean.TRUE);
    rawMap.put("b", Boolean.TRUE);
    assertEquals(2, PSWorkflowUtilsBase.filterList(rawList, rawMap).size());
    assertEquals(2, PSWorkflowUtilsBase.intersectLists(rawList, rawList).size());
    assertEquals("a,b", PSWorkflowUtilsBase.listToDelimitedString(rawList, ","));
    assertEquals(2, PSWorkflowUtilsBase.caseInsensitiveUniqueList(rawList).size());
  }
}
