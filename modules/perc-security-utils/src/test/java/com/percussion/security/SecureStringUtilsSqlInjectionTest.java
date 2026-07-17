/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for SQL-injection barriers on {@link SecureStringUtils} (CodeQL {@code
 * java/sql-injection}, T042).
 */
@DisplayName("SecureStringUtils SQL object name / metadata token barriers (T042)")
class SecureStringUtilsSqlInjectionTest {

  @Test
  void requireSqlObjectNameAcceptsPlainIdentifier() {
    assertEquals("CONTENTSTATUS", SecureStringUtils.requireSqlObjectName("CONTENTSTATUS"));
    assertEquals("psx_templates", SecureStringUtils.requireSqlObjectName("psx_templates"));
  }

  @Test
  void requireSqlObjectNameRejectsInjectionFragments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSqlObjectName("t; DROP TABLE users--"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSqlObjectName("t' OR '1'='1"));
    assertThrows(IllegalArgumentException.class, () -> SecureStringUtils.requireSqlObjectName(""));
    assertThrows(
        IllegalArgumentException.class, () -> SecureStringUtils.requireSqlObjectName(null));
  }

  @Test
  void requireSqlObjectNameOrNullAllowsBlank() {
    assertNull(SecureStringUtils.requireSqlObjectNameOrNull(null));
    assertNull(SecureStringUtils.requireSqlObjectNameOrNull("  "));
    assertEquals("dbo", SecureStringUtils.requireSqlObjectNameOrNull("dbo"));
  }

  @Test
  void requireSafeMetadataTokenAcceptsNamespacedNames() {
    assertEquals(
        "dcterms:created", SecureStringUtils.requireSafeMetadataToken("dcterms:created"));
    assertEquals("linktext_lower", SecureStringUtils.requireSafeMetadataToken("linktext_lower"));
  }

  @Test
  void requireSafeMetadataTokenRejectsSqlMetacharacters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSafeMetadataToken("name; drop table x"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSafeMetadataToken("name' OR '1'='1"));
    assertThrows(
        IllegalArgumentException.class, () -> SecureStringUtils.requireSafeMetadataToken("a b"));
  }

  @Test
  void requireSingleSqlStatementRejectsStackedQueriesOnly() {
    assertEquals("SELECT 1", SecureStringUtils.requireSingleSqlStatement("SELECT 1;"));
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireSingleSqlStatement("SELECT 1; DROP TABLE t"));
    // Comments allowed on general path (string literals / hints).
    assertEquals(
        "SELECT * FROM t WHERE c = 'a--b'",
        SecureStringUtils.requireSingleSqlStatement("SELECT * FROM t WHERE c = 'a--b'"));
  }

  @Test
  void requireFactorySqlStatementAlsoRejectsComments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureStringUtils.requireFactorySqlStatement("SELECT 1 -- x"));
  }
}
