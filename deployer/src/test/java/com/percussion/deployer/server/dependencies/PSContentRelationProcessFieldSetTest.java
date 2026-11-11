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

package com.percussion.deployer.server.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSInlineLinkField;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSField;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the {@code processFieldSet} helpers extracted from {@link
 * PSContentRelationDependencyHandler} (issue #6040, complexity.cognitive residual). The full
 * install path requires a live CMS; these tests lock the behavior of the extracted leaf helpers so
 * the refactor cannot silently change inline-link fix-up semantics.
 */
public class PSContentRelationProcessFieldSetTest {

  @Test
  @DisplayName("registerInlineField maps table to lower-cased columns and column to submit name")
  public void registerInlineFieldCollectsInlineColumn() {
    // given an inline-capable field backed by a back-end column
    PSField field = mock(PSField.class);
    PSBackEndColumn beCol = mock(PSBackEndColumn.class);
    PSBackEndTable beTable = mock(PSBackEndTable.class);
    when(field.mayHaveInlineLinks()).thenReturn(true);
    when(field.getLocator()).thenReturn(beCol);
    when(field.getSubmitName()).thenReturn("body");
    when(beCol.getTable()).thenReturn(beTable);
    when(beTable.getTable()).thenReturn("CONTENT");
    when(beCol.getColumn()).thenReturn("BODY");
    Map<String, List<String>> inlineFieldMap = new HashMap<>();
    Map<String, String> fieldMap = new HashMap<>();

    // when the field is registered
    PSContentRelationDependencyHandler.registerInlineField(field, inlineFieldMap, fieldMap);

    // then the table maps to the lower-cased column and the column maps to the submit name
    assertEquals(List.of("body"), inlineFieldMap.get("CONTENT"));
    assertEquals("body", fieldMap.get("BODY"));
  }

  @Test
  @DisplayName("registerInlineField accumulates multiple columns on the same table")
  public void registerInlineFieldAccumulatesSameTableColumns() {
    // given two inline-capable fields on the same table
    Map<String, List<String>> inlineFieldMap = new HashMap<>();
    Map<String, String> fieldMap = new HashMap<>();
    PSContentRelationDependencyHandler.registerInlineField(
        inlineField("CONTENT", "BODY", "body"), inlineFieldMap, fieldMap);

    // when a second field on the same table is registered
    PSContentRelationDependencyHandler.registerInlineField(
        inlineField("CONTENT", "TITLE", "title"), inlineFieldMap, fieldMap);

    // then both columns accumulate under the one table entry
    assertEquals(List.of("body", "title"), inlineFieldMap.get("CONTENT"));
    assertEquals("title", fieldMap.get("TITLE"));
  }

  @Test
  @DisplayName("registerInlineField ignores fields without inline-link support")
  public void registerInlineFieldIgnoresNonInlineField() {
    // given a field that cannot hold inline links
    PSField field = mock(PSField.class);
    when(field.mayHaveInlineLinks()).thenReturn(false);
    Map<String, List<String>> inlineFieldMap = new HashMap<>();
    Map<String, String> fieldMap = new HashMap<>();

    // when the field is registered
    PSContentRelationDependencyHandler.registerInlineField(field, inlineFieldMap, fieldMap);

    // then both maps stay empty
    assertTrue(inlineFieldMap.isEmpty());
    assertTrue(fieldMap.isEmpty());
  }

  @Test
  @DisplayName("registerInlineField ignores fields without a back-end column locator")
  public void registerInlineFieldIgnoresNonColumnLocator() {
    // given an inline-capable field with no back-end column mapping
    PSField field = mock(PSField.class);
    when(field.mayHaveInlineLinks()).thenReturn(true);
    when(field.getLocator()).thenReturn(null);
    Map<String, List<String>> inlineFieldMap = new HashMap<>();
    Map<String, String> fieldMap = new HashMap<>();

    // when the field is registered
    PSContentRelationDependencyHandler.registerInlineField(field, inlineFieldMap, fieldMap);

    // then both maps stay empty
    assertTrue(inlineFieldMap.isEmpty());
    assertTrue(fieldMap.isEmpty());
  }

  @Test
  @DisplayName("resolveChildSysId returns the SYSID value for child rows")
  public void resolveChildSysIdReturnsValueForChildRow() {
    // given a row carrying a non-blank child-row id
    PSJdbcRowData row = rowWith(IPSConstants.CHILD_ITEM_PKEY, "42");

    // when the child id is resolved
    String sysId = PSContentRelationDependencyHandler.resolveChildSysId(row);

    // then the id is returned
    assertEquals("42", sysId);
  }

  @Test
  @DisplayName("resolveChildSysId returns null when the SYSID column is missing or blank")
  public void resolveChildSysIdReturnsNullForParentRow() {
    // given a parent row with no SYSID column and rows with blank SYSID values
    // when the child ids are resolved
    // then every resolution is null
    assertNull(PSContentRelationDependencyHandler.resolveChildSysId(rowWith("CONTENTID", "7")));
    assertNull(PSContentRelationDependencyHandler.resolveChildSysId(rowWith(IPSConstants.CHILD_ITEM_PKEY, null)));
    assertNull(PSContentRelationDependencyHandler.resolveChildSysId(rowWith(IPSConstants.CHILD_ITEM_PKEY, "   ")));
  }

  @Test
  @DisplayName("isInlineColumn matches case-insensitively against the inline list")
  public void isInlineColumnMatchesCaseInsensitively() {
    // given a lower-cased inline column list
    List<String> colList = List.of("body", "title");

    // when cursor column names are tested
    // then matching is case-insensitive and non-listed columns are rejected
    assertTrue(PSContentRelationDependencyHandler.isInlineColumn("BODY", colList));
    assertTrue(PSContentRelationDependencyHandler.isInlineColumn("Title", colList));
    assertFalse(PSContentRelationDependencyHandler.isInlineColumn("OTHER", colList));
  }

  @Test
  @DisplayName("matchesInlineRelationship follows the field-name match rule")
  public void matchesInlineRelationshipMatchesOwningField() {
    // given an inline-relationship id built for the body field of child row 42
    String inlineRelText = PSInlineLinkField.makeInlineRelationshipId("body", "42");

    // when the match rule is evaluated
    // then the owning field matches, other fields do not, and blank values never match
    assertTrue(PSContentRelationDependencyHandler.matchesInlineRelationship("body", inlineRelText));
    assertTrue(
        PSContentRelationDependencyHandler.matchesInlineRelationship("BODY", inlineRelText));
    assertFalse(
        PSContentRelationDependencyHandler.matchesInlineRelationship("title", inlineRelText));
    assertFalse(PSContentRelationDependencyHandler.matchesInlineRelationship("body", null));
    assertFalse(PSContentRelationDependencyHandler.matchesInlineRelationship("body", "   "));
  }

  private static PSField inlineField(String table, String column, String submitName) {
    PSField field = mock(PSField.class);
    PSBackEndColumn beCol = mock(PSBackEndColumn.class);
    PSBackEndTable beTable = mock(PSBackEndTable.class);
    when(field.mayHaveInlineLinks()).thenReturn(true);
    when(field.getLocator()).thenReturn(beCol);
    when(field.getSubmitName()).thenReturn(submitName);
    when(beCol.getTable()).thenReturn(beTable);
    when(beTable.getTable()).thenReturn(table);
    when(beCol.getColumn()).thenReturn(column);
    return field;
  }

  private static PSJdbcRowData rowWith(String name, String value) {
    List<PSJdbcColumnData> cols = new ArrayList<>();
    cols.add(new PSJdbcColumnData(name, value));
    return new PSJdbcRowData(cols.iterator(), PSJdbcRowData.ACTION_UPDATE);
  }
}
