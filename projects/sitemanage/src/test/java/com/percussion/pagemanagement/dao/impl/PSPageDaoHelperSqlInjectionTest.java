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
package com.percussion.pagemanagement.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.searchmanagement.data.PSSearchCriteria;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link PSPageDaoHelper#formGetByStatusSQLQuery} (CodeQL {@code
 * java/sql-injection}, T042, US3).
 *
 * <p><strong>Background.</strong> The pre-fix code concatenated user-supplied values from {@link
 * PSSearchCriteria#getSearchFields()} directly into the SQL string (e.g., {@code "AND
 * P.TEMPLATEID='" + value + "'"}). The fix uses named-parameter placeholders and binds the values
 * via {@code setParameter}.
 *
 * <p><strong>Fail-then-pass coverage (Constitution III).</strong> The pre-fix code produced SQL
 * containing the raw user value (e.g., {@code AND P.TEMPLATEID='malicious'}). The post-fix code
 * produces SQL with named-parameter placeholders (e.g., {@code AND P.TEMPLATEID = :templateid}) and
 * records the value separately in the params map. The test asserts the SQL string does NOT contain
 * the user value, which would fail on pre-fix and pass on post-fix.
 */
@DisplayName("PSPageDaoHelper.formGetByStatusSQLQuery — SQL injection (CWE-89) regression tests")
class PSPageDaoHelperSqlInjectionTest {

  private PSPageDaoHelper m_helper;
  private PSSearchCriteria m_criteria;
  private Map<String, Object> m_params;

  @BeforeEach
  void setUp() {
    m_helper = new PSPageDaoHelper(null, null, null);
    m_criteria = new PSSearchCriteria();
    m_criteria.setFolderPath("/Sites/foo");
    // Initialize an empty searchFields map via the setter; getSearchFields
    // returns an unmodifiable view of the internal map, so we must go
    // through setSearchFields for each test that needs to add fields.
    m_criteria.setSearchFields(new HashMap<>());
    m_params = new HashMap<>();
  }

  @Test
  @DisplayName("templateid value is bound as :templateid parameter, not concatenated")
  void testTemplateIdUsesNamedParameter() {
    var fields = new HashMap<String, String>();
    fields.put("templateid", "x'; DROP TABLE users; --");
    m_criteria.setSearchFields(fields);
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);

    // The SQL must contain the named placeholder, not the raw user value.
    assertTrue(
        result.contains(":templateid"),
        "SQL must use a named parameter placeholder for templateid, got: " + result);
    assertFalse(
        result.contains("x'; DROP TABLE"),
        "SQL must NOT contain the raw user-supplied templateid value, got: " + result);
    // The value is recorded in the params map for later setParameter binding.
    assertEquals("x'; DROP TABLE users; --", m_params.get("templateid"));
  }

  @Test
  @DisplayName("sys_contenttypeid is bound as a long parameter, not concatenated")
  void testSysContentTypeIdUsesNamedParameter() {
    var fields = new HashMap<String, String>();
    fields.put("sys_contenttypeid", "123");
    m_criteria.setSearchFields(fields);
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);

    assertTrue(
        result.contains(":contenttypeid"),
        "SQL must use a named parameter placeholder for sys_contenttypeid, got: " + result);
    assertFalse(
        result.contains("'123'"),
        "SQL must NOT contain the raw user-supplied contenttypeid value, got: " + result);
    assertEquals(123L, m_params.get("contenttypeid"));
  }

  @Test
  @DisplayName("sys_contentstateid is bound as a long parameter")
  void testSysContentStateIdUsesNamedParameter() {
    var fields = new HashMap<String, String>();
    fields.put("sys_contentstateid", "5");
    m_criteria.setSearchFields(fields);
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);

    assertTrue(result.contains(":contentstateid"));
    assertEquals(5L, m_params.get("contentstateid"));
  }

  @Test
  @DisplayName("sys_workflowid is bound as a long parameter")
  void testSysWorkflowIdUsesNamedParameter() {
    var fields = new HashMap<String, String>();
    fields.put("sys_workflowid", "42");
    m_criteria.setSearchFields(fields);
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);

    assertTrue(result.contains(":workflowappid"));
    assertEquals(42L, m_params.get("workflowappid"));
  }

  @Test
  @DisplayName(
      "sys_contentlastmodifier is bound as a LIKE parameter with % wildcards applied at the"
          + " boundary")
  void testSysContentLastModifierUsesNamedParameter() {
    var fields = new HashMap<String, String>();
    fields.put("sys_contentlastmodifier", "admin");
    m_criteria.setSearchFields(fields);
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);

    assertTrue(
        result.contains(":contentlastmodifier"),
        "SQL must use a named parameter placeholder for sys_contentlastmodifier, got: " + result);
    // The SQL must NOT embed the user value with surrounding % wildcards
    // (the pre-fix code did exactly that: `LIKE '%admin%'`).
    assertFalse(
        result.contains("'%admin%'"),
        "SQL must NOT contain the literal %admin% pattern, got: " + result);
    // The bound value carries the wildcards; the LIKE clause is parameterized.
    assertEquals("%admin%", m_params.get("contentlastmodifier"));
  }

  @Test
  @DisplayName("classic SQL injection payload in templateid is not reflected in SQL")
  void testClassicSqlInjectionPayload() {
    // The canonical SQL injection payload. The pre-fix code would produce
    // SQL containing: AND P.TEMPLATEID='' OR 1=1 --'
    // The post-fix code produces: AND P.TEMPLATEID = :templateid
    // and stores the payload in the params map for parameter binding.
    String malicious = "' OR '1'='1";
    var fields = new HashMap<String, String>();
    fields.put("templateid", malicious);
    m_criteria.setSearchFields(fields);
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, "SELECT 1", m_params);

    assertNotNull(result);
    assertFalse(
        result.contains(malicious),
        "the malicious payload must not appear in the SQL string, got: " + result);
    assertFalse(
        result.toUpperCase().contains(" OR "), "no 'OR' clause may be injected, got: " + result);
    assertTrue(result.contains(":templateid"));
    assertEquals(malicious, m_params.get("templateid"));
  }

  @Test
  @DisplayName("empty searchFields produces an unchanged SQL string")
  void testNoSearchFieldsLeavesSqlUnchanged() {
    String sql = "SELECT 1 FROM dual";
    String result = m_helper.formGetByStatusSQLQuery(m_criteria, sql, m_params);
    assertEquals(sql, result);
    assertTrue(m_params.isEmpty());
  }

  @Test
  @DisplayName("multiple search fields are all parameterized")
  void testMultipleSearchFieldsAllParameterized() {
    var fields = new HashMap<String, String>();
    fields.put("templateid", "t1");
    fields.put("sys_contenttypeid", "100");
    fields.put("sys_contentstateid", "1");
    fields.put("sys_workflowid", "9");
    fields.put("sys_contentlastmodifier", "alice");
    m_criteria.setSearchFields(fields);

    String result = m_helper.formGetByStatusSQLQuery(m_criteria, "SELECT 1", m_params);

    assertTrue(result.contains(":templateid"));
    assertTrue(result.contains(":contenttypeid"));
    assertTrue(result.contains(":contentstateid"));
    assertTrue(result.contains(":workflowappid"));
    assertTrue(result.contains(":contentlastmodifier"));
    // None of the user values should appear verbatim in the SQL.
    assertFalse(result.contains("'t1'"));
    assertFalse(result.contains("'100'"));
    assertFalse(result.contains("'1'"));
    assertFalse(result.contains("'9'"));
    assertFalse(result.contains("'alice'"));
    // 5 named parameters should be recorded.
    assertEquals(5, m_params.size());
  }
}
