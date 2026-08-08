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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for CodeQL {@code java/sql-injection} alerts #1941 / #1942 on {@link
 * PSDatabasePoolDatabaseMetaData} JDBC metadata wrappers.
 */
@DisplayName("PSDatabasePoolDatabaseMetaData SQL identifier barriers (#1941/#1942)")
class PSDatabasePoolDatabaseMetaDataSqlInjectionTest {

  private DatabaseMetaData delegate;
  private PSDatabasePoolDatabaseMetaData wrapper;

  @BeforeEach
  void setUp() throws Exception {
    delegate = mock(DatabaseMetaData.class);
    when(delegate.supportsCatalogsInDataManipulation()).thenReturn(true);
    when(delegate.supportsSchemasInDataManipulation()).thenReturn(true);
    when(delegate.storesUpperCaseIdentifiers()).thenReturn(false);
    when(delegate.storesLowerCaseIdentifiers()).thenReturn(false);
    wrapper = new PSDatabasePoolDatabaseMetaData(delegate);
  }

  @Test
  void getFixedupIdentifierRejectsInjectionFragments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> wrapper.getFixedupIdentifier("t; DROP TABLE users--"));
    assertThrows(
        IllegalArgumentException.class, () -> wrapper.getFixedupIdentifier("t' OR '1'='1"));
  }

  @Test
  void getFixedupIdentifierAllowsJdbcWildcards() {
    assertEquals("PSX_%", wrapper.getFixedupIdentifier("PSX_%"));
    assertEquals("%", wrapper.getFixedupIdentifier("%"));
    assertNull(wrapper.getFixedupIdentifier(null));
  }

  @Test
  void getColumnsDelegatesAfterValidation() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(delegate.getColumns(isNull(), isNull(), eq("CONTENTSTATUS"), eq("%"))).thenReturn(rs);

    ResultSet out = wrapper.getColumns(null, null, "CONTENTSTATUS", "%");
    assertSame(rs, out);
    verify(delegate).getColumns(null, null, "CONTENTSTATUS", "%");
  }

  @Test
  void getColumnsRejectsUnsafeTablePattern() {
    assertThrows(
        IllegalArgumentException.class,
        () -> wrapper.getColumns(null, null, "t; DROP TABLE x", "%"));
  }

  @Test
  void getPrimaryKeysDelegatesAfterValidation() throws Exception {
    ResultSet rs = mock(ResultSet.class);
    when(delegate.getPrimaryKeys(isNull(), isNull(), eq("CONTENTSTATUS"))).thenReturn(rs);

    ResultSet out = wrapper.getPrimaryKeys(null, null, "CONTENTSTATUS");
    assertSame(rs, out);
    verify(delegate).getPrimaryKeys(null, null, "CONTENTSTATUS");
  }

  @Test
  void getPrimaryKeysRejectsUnsafeTableName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> wrapper.getPrimaryKeys(null, null, "t' OR '1'='1"));
  }
}
