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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.design.objectstore.PSAcl;
import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.design.objectstore.PSAuthentication;
import com.percussion.design.objectstore.PSDirectory;
import com.percussion.util.PSCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for residual {@code com.percussion.security} / design.catalog.security Xlint
 * rawtypes cleanup (issue #3182, parent epic #2022).
 *
 * <p>Covers pure logic that does not require a live security cataloger stack: typed attribute maps
 * on {@link PSBackEndConnection}, ACL entry maps on {@link PSAclHandler}, return-attribute merge on
 * {@link PSDirectoryDefinition} (including the prior inverted null check), and multi-value LDAP
 * filter building on {@link PSJndiUtils#buildFilter(Map)}.
 */
@Tag("UnitTest")
public class PSSecurityPackageTypedTest {

  @Test
  void backEndConnectionExposesTypedUserAttributeMap() {
    Properties props = new Properties();
    props.setProperty(PSBackEndConnection.PROPS_DATASOURCE_NAME, "rxdefault");
    props.setProperty(PSBackEndConnection.PROPS_TABLE_NAME, "USERLOGIN");
    props.setProperty(PSBackEndConnection.PROPS_UID_COLUMN, "USERID");
    props.setProperty(PSBackEndConnection.PROPS_PW_COLUMN, "PASSWORD");
    props.setProperty("email", "EMAILCOL");
    props.setProperty("dept", "DEPTCOL");

    PSBackEndConnection conn = new PSBackEndConnection(props);

    assertEquals("EMAILCOL", conn.getUserAttribute("email"));
    assertEquals("DEPTCOL", conn.getUserAttribute("dept"));
    // password column is not exposed as a user attribute
    assertNull(conn.getUserAttribute(PSBackEndConnection.PROPS_PW_COLUMN));
    assertNull(conn.getUserAttribute("PASSWORD"));

    List<String> names = new ArrayList<>();
    Iterator<String> it = conn.getUserAttributeNames();
    while (it.hasNext()) {
      names.add(it.next());
    }
    assertTrue(names.contains("email"));
    assertTrue(names.contains("dept"));
    assertFalse(names.contains(PSBackEndConnection.PROPS_PW_COLUMN));

    String sql = conn.prepareStatement(List.of("USERID", "EMAILCOL"), "USERID", "USERLOGIN");
    assertEquals("SELECT USERID, EMAILCOL FROM USERLOGIN WHERE USERID=?", sql);
  }

  @Test
  void directoryDefinitionMergesDirectoryAndAdditionalAttributes() throws Exception {
    PSCollection dirAttrs = new PSCollection(String.class);
    dirAttrs.add("mail");
    dirAttrs.add("cn");

    PSDirectory directory =
        new PSDirectory(
            "corpDir",
            PSDirectory.CATALOG_SHALLOW,
            "com.sun.jndi.ldap.LdapCtxFactory",
            "auth1",
            "ldap://localhost:389/dc=example,dc=com",
            dirAttrs);
    PSAuthentication auth =
        new PSAuthentication("auth1", "simple", "bindUser", "uid", "secret", null);

    PSDirectoryDefinition def = new PSDirectoryDefinition(auth, directory);

    Set<String> additional = new HashSet<>();
    additional.add("sn");
    additional.add("mail"); // duplicate of directory attr

    Set<String> merged = def.getReturnAttributeNames(additional);
    assertEquals(3, merged.size());
    assertTrue(merged.contains("mail"));
    assertTrue(merged.contains("cn"));
    assertTrue(merged.contains("sn"));

    // null additional must not NPE and must still return directory attributes
    Set<String> fromDirOnly = def.getReturnAttributeNames(null);
    assertEquals(2, fromDirOnly.size());
    assertTrue(fromDirOnly.contains("mail"));
    assertTrue(fromDirOnly.contains("cn"));
  }

  @Test
  void aclHandlerMapsTypedUserGroupRoleEntries() {
    PSAcl acl = new PSAcl();
    PSCollection entries = acl.getEntries();

    PSAclEntry user = new PSAclEntry("Alice", PSAclEntry.ACE_TYPE_USER);
    user.setAccessLevel(PSAclEntry.AACE_DATA_QUERY);
    entries.add(user);

    PSAclEntry group = new PSAclEntry("Editors", PSAclEntry.ACE_TYPE_GROUP);
    group.setAccessLevel(PSAclEntry.AACE_DATA_QUERY | PSAclEntry.AACE_DATA_UPDATE);
    entries.add(group);

    PSAclEntry role = new PSAclEntry("Admin", PSAclEntry.ACE_TYPE_ROLE);
    role.setAccessLevel(PSAclEntry.AACE_DATA_QUERY);
    entries.add(role);

    PSAclHandler handler = new PSAclHandler(acl);

    List<PSEntry> users = collect(handler.getAclEntries(PSAclEntry.ACE_TYPE_USER));
    assertEquals(1, users.size());
    assertEquals("Alice", users.get(0).getName());
    assertTrue(users.get(0) instanceof PSUserEntry);

    List<PSEntry> groups = collect(handler.getAclEntries(PSAclEntry.ACE_TYPE_GROUP));
    assertEquals(1, groups.size());
    assertEquals("Editors", groups.get(0).getName());
    assertTrue(groups.get(0) instanceof PSGroupEntry);

    List<PSEntry> roles = collect(handler.getAclEntries(PSAclEntry.ACE_TYPE_ROLE));
    assertEquals(1, roles.size());
    assertEquals("Admin", roles.get(0).getName());
    assertTrue(roles.get(0) instanceof PSRoleEntry);
  }

  @Test
  void jndiBuildFilterAcceptsTypedMultiValueCollections() {
    Map<String, Object> values = new HashMap<>();
    values.put("objectClass", "person");
    values.put("cn", List.of("alice", "bob"));

    String filter = PSJndiUtils.buildFilter(values);
    assertNotNull(filter);
    assertTrue(filter.startsWith("(&"));
    assertTrue(filter.contains("(objectClass=person)"));
    assertTrue(filter.contains("(cn=alice)"));
    assertTrue(filter.contains("(cn=bob)"));
    assertTrue(filter.contains("(|"));
  }

  @Test
  void jndiBuildFilterEmptyCollectionUsesWildcard() {
    Map<String, Object> values = new HashMap<>();
    values.put("sn", List.of());

    String filter = PSJndiUtils.buildFilter(values);
    assertEquals("(&(sn=*))", filter);
  }

  private static List<PSEntry> collect(Iterator<PSEntry> it) {
    List<PSEntry> list = new ArrayList<>();
    while (it.hasNext()) {
      list.add(it.next());
    }
    return list;
  }
}
