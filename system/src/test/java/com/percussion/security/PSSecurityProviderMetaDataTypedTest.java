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
package com.percussion.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAttributeList;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed security-provider metadata result sets and related attribute maps
 * (issue #2299 batch 1: security provider metadata rawtypes).
 */
@Tag("UnitTest")
public class PSSecurityProviderMetaDataTypedTest {

  @Test
  void emptyServersResultSetHasTypedColumnAndNoRows() throws Exception {
    PSSecurityProviderMetaData meta = new PSWebServerProviderMetaData();
    ResultSet rs = meta.getServers();
    assertNotNull(rs);
    assertEquals(1, rs.getMetaData().getColumnCount());
    assertEquals("SERVER_NAME", rs.getMetaData().getColumnName(1));
    assertFalse(rs.next());
    rs.close();
  }

  @Test
  void emptyObjectTypesResultSetHasTypedColumnAndNoRows() throws Exception {
    // Default web-server metadata does not support object-type enumeration; empty RS from base.
    PSSecurityProviderMetaData meta = new PSWebServerProviderMetaData();
    ResultSet rs = meta.getObjectTypes();
    assertNotNull(rs);
    assertEquals(1, rs.getMetaData().getColumnCount());
    assertEquals("OBJECT_TYPE", rs.getMetaData().getColumnName(1));
    assertFalse(rs.next());
    rs.close();
  }

  @Test
  void emptyObjectsResultSetHasThreeColumnsAndNoRows() throws Exception {
    PSSecurityProviderMetaData meta = new PSWebServerProviderMetaData();
    ResultSet rs = meta.getObjects(null);
    assertNotNull(rs);
    assertEquals(3, rs.getMetaData().getColumnCount());
    assertEquals("OBJECT_TYPE", rs.getMetaData().getColumnName(1));
    assertEquals("OBJECT_ID", rs.getMetaData().getColumnName(2));
    assertEquals("OBJECT_NAME", rs.getMetaData().getColumnName(3));
    assertFalse(rs.next());
    rs.close();
  }

  @Test
  void webServerAttributesResultSetListsKnownClientAttributes() throws Exception {
    PSWebServerProviderMetaData meta = new PSWebServerProviderMetaData();
    assertTrue(meta.supportsGetAttributes());

    ResultSet rs = meta.getAttributes(null);
    assertNotNull(rs);
    assertEquals(3, rs.getMetaData().getColumnCount());
    assertEquals("OBJECT_TYPE", rs.getMetaData().getColumnName(1));
    assertEquals("ATTRIBUTE_NAME", rs.getMetaData().getColumnName(2));
    assertEquals("ATTRIBUTE_DESC", rs.getMetaData().getColumnName(3));

    List<String> names = new ArrayList<>();
    while (rs.next()) {
      names.add(rs.getString("ATTRIBUTE_NAME"));
    }
    rs.close();

    assertTrue(names.contains("Client/Subject"));
    assertTrue(names.contains("Client/CN"));
    assertTrue(names.contains("keyStrength"));
    assertEquals(16, names.size());
  }

  @Test
  void userAttributesJoinsMultiValuesAndTypedGetString() {
    PSAttributeList list = new PSAttributeList();
    list.setAttribute("email", List.of("a@example.com", "b@example.com"));

    PSUserAttributes map = new PSUserAttributes(list);
    assertEquals("a@example.com,b@example.com", map.getString("email"));
    assertEquals(1, map.size());
  }

  @Test
  void authenticationFailedExJoinsProviderMessages() {
    List<PSAuthenticationFailedException> failures = new ArrayList<>();
    failures.add(new PSAuthenticationFailedException("NT", "inst-a", "user1"));
    failures.add(new PSAuthenticationFailedException("DBMS", "inst-b", "user1", "bad password"));

    Iterator<PSAuthenticationFailedException> it = failures.iterator();
    PSAuthenticationFailedExException ex = new PSAuthenticationFailedExException(it);
    String msg = ex.getLocalizedMessage();
    assertNotNull(msg);
    assertFalse(msg.isEmpty());
  }
}
